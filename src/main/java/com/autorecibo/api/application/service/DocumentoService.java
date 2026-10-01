package com.autorecibo.api.application.service;

import com.autorecibo.api.domain.model.Documento;
import com.autorecibo.api.domain.model.ItemDocumento;
import com.autorecibo.api.domain.model.Usuario;
import com.autorecibo.api.domain.model.ItemCatalogo;
import com.autorecibo.api.application.dto.DocumentoRequest;
import com.autorecibo.api.application.dto.DocumentoResponse;
import com.autorecibo.api.application.dto.DocumentoResumoResponse;
import com.autorecibo.api.application.dto.ItemDocumentoRequest;
import com.autorecibo.api.application.dto.ItemDocumentoResponse;
import com.autorecibo.api.infrastructure.persistence.repository.UsuarioRepository;
import com.autorecibo.api.infrastructure.persistence.repository.DocumentoRepository;
import com.autorecibo.api.infrastructure.persistence.repository.ItemCatalogoRepository;
import com.autorecibo.api.application.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Regras do documento. O servidor recalcula TUDO a partir do catálogo; nada de valor vem do cliente.
 *
 * Regra de arredondamento (o frontend deve replicar para os totais baterem):
 *   bruto    = preço × quantidade, arredondado a 2 casas (HALF_UP)
 *   desconto = bruto × % / 100,    arredondado a 2 casas (HALF_UP)
 *   líquido  = bruto − desconto
 *   totais   = soma dos valores JÁ arredondados de cada linha
 */
@Service
@RequiredArgsConstructor
public class DocumentoService {

    private static final Set<Integer> DESCONTOS_PERMITIDOS = Set.of(0, 5, 10, 15, 30);
    private static final BigDecimal CEM = BigDecimal.valueOf(100);

    private final UsuarioRepository usuarioRepository;
    private final ItemCatalogoRepository catalogoRepository;
    private final DocumentoRepository documentoRepository;

    @Transactional(readOnly = true)
    public DocumentoResponse preview(String email, DocumentoRequest req) {
        return paraResponse(montar(usuario(email), req));
    }

    @Transactional
    public DocumentoResponse emitir(String email, DocumentoRequest req) {
        Usuario usuario = usuario(email);
        Documento doc = montar(usuario, req);
        doc.setNumero(documentoRepository.maiorNumero(usuario.getId()) + 1);
        doc.setEmitidoEm(LocalDateTime.now());
        try {
            documentoRepository.saveAndFlush(doc);
        } catch (DataIntegrityViolationException e) {
            // duas emissões simultâneas disputaram o mesmo número; a constraint do banco é a garantia
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Não foi possível numerar o documento. Tente novamente.");
        }
        return paraResponse(doc);
    }

    @Transactional(readOnly = true)
    public List<DocumentoResumoResponse> listar(String email) {
        Usuario usuario = usuario(email);
        return documentoRepository.findTop50ByUsuarioIdOrderByEmitidoEmDesc(usuario.getId()).stream()
                .map(d -> new DocumentoResumoResponse(d.getId(), d.getNumero(), d.getTipo(), d.getClienteNome(),
                        d.getClienteDocumento(), d.getTotal(), d.getEmitidoEm()))
                .toList();
    }

    /** 404 (e não 403) quando o documento é de outro usuário, para não revelar que ele existe. */
    @Transactional(readOnly = true)
    public DocumentoResponse buscar(String email, UUID id) {
        Usuario usuario = usuario(email);
        Documento doc = documentoRepository.findByIdAndUsuarioId(id, usuario.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Documento não encontrado."));
        return paraResponse(doc);
    }

    // ---------------------------------------------------------------- internos

    private Usuario usuario(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão inválida."));
    }

    /** Monta o documento (sem persistir) a partir do pedido, calculando tudo no servidor. */
    private Documento montar(Usuario usuario, DocumentoRequest req) {
        Set<UUID> ids = req.itens().stream().map(ItemDocumentoRequest::itemCatalogoId).collect(Collectors.toSet());

        // Só enxerga itens do próprio usuário: id alheio é tratado como inexistente.
        Map<UUID, ItemCatalogo> catalogo = catalogoRepository.findAllByIdInAndUsuario_Id(ids, usuario.getId()).stream()
                .collect(Collectors.toMap(ItemCatalogo::getId, Function.identity()));
        if (catalogo.size() != ids.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Há itens que não existem no seu catálogo.");
        }

        Documento doc = new Documento();
        doc.setUsuario(usuario);
        doc.setTipo(req.tipo());
        doc.setClienteNome(req.clienteNome().trim());
        doc.setClienteDocumento(normalizarDocumento(req.clienteDocumento()));
        doc.setObservacoes(req.observacoes() == null || req.observacoes().isBlank() ? null : req.observacoes().trim());

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal descontos = BigDecimal.ZERO;
        int ordem = 0;

        for (ItemDocumentoRequest entrada : req.itens()) {
            int pct = entrada.descontoPercentual() == null ? 0 : entrada.descontoPercentual();
            if (!DESCONTOS_PERMITIDOS.contains(pct)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Desconto inválido. Use 0, 5, 10, 15 ou 30.");
            }
            ItemCatalogo origem = catalogo.get(entrada.itemCatalogoId());

            BigDecimal bruto = origem.getPreco().multiply(entrada.quantidade()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal desconto = bruto.multiply(BigDecimal.valueOf(pct)).divide(CEM, 2, RoundingMode.HALF_UP);

            ItemDocumento item = new ItemDocumento();
            item.setDocumento(doc);
            item.setItemCatalogoId(origem.getId());
            item.setOrdem(ordem++);
            item.setNome(origem.getNome());
            item.setPrecoUnitario(origem.getPreco().setScale(2, RoundingMode.HALF_UP));
            item.setQuantidade(entrada.quantidade());
            item.setDescontoPercentual(pct);
            item.setValorBruto(bruto);
            item.setValorDesconto(desconto);
            item.setValorLiquido(bruto.subtract(desconto));
            doc.getItens().add(item);

            subtotal = subtotal.add(bruto);
            descontos = descontos.add(desconto);
        }

        doc.setSubtotal(subtotal);
        doc.setTotalDescontos(descontos);
        doc.setTotal(subtotal.subtract(descontos));
        return doc;
    }

    private String normalizarDocumento(String bruto) {
        if (bruto == null || bruto.isBlank()) return null;
        String digitos = bruto.replaceAll("\\D", "");
        if (digitos.length() != 11 && digitos.length() != 14) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CPF ou CNPJ do cliente inválido.");
        }
        return digitos;
    }

    private DocumentoResponse paraResponse(Documento d) {
        List<ItemDocumentoResponse> itens = d.getItens().stream()
                .map(i -> new ItemDocumentoResponse(i.getItemCatalogoId(), i.getNome(), i.getPrecoUnitario(),
                        i.getQuantidade(), i.getDescontoPercentual(),
                        i.getValorBruto(), i.getValorDesconto(), i.getValorLiquido()))
                .toList();
        return new DocumentoResponse(d.getId(), d.getNumero(), d.getTipo(), d.getClienteNome(),
                d.getClienteDocumento(), d.getObservacoes(), itens,
                d.getSubtotal(), d.getTotalDescontos(), d.getTotal(), d.getEmitidoEm());
    }
}