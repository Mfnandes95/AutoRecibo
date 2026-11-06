package com.autorecibo.api.application.service;

import com.autorecibo.api.domain.model.ItemCatalogo;
import com.autorecibo.api.domain.model.Usuario;
import com.autorecibo.api.infrastructure.persistence.repository.ItemCatalogoRepository;
import com.autorecibo.api.infrastructure.persistence.repository.UsuarioRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogoService {

    private final ItemCatalogoRepository catalogoRepository;
    private final UsuarioRepository usuarioRepository;

    public record ItemCatalogoResponse(
            UUID id,
            String nome,
            String descricao,
            BigDecimal preco
    ) {}

    public record NovoItemRequest(
            @NotBlank(message = "O nome do item é obrigatório.")
            String nome,
            String descricao,
            @NotNull(message = "O preço é obrigatório.")
            @PositiveOrZero(message = "O preço não pode ser negativo.")
            BigDecimal preco
    ) {}

    public record ItemPadrao(String nome, String descricao, BigDecimal preco) {}

    public static final List<ItemPadrao> ITENS_PADRAO = List.of(
            new ItemPadrao("Formatação e Instalação de SO", "Instalação limpa de sistema operacional com drivers e programas básicos", new BigDecimal("120.00")),
            new ItemPadrao("Manutenção Preventiva de Computador", "Limpeza interna, troca de pasta térmica e testes de estabilidade", new BigDecimal("85.00")),
            new ItemPadrao("Configuração de Rede e Wi-Fi", "Instalação de roteador, cabeamento e configuração de segurança", new BigDecimal("150.00")),
            new ItemPadrao("Troca de Tela de Notebook", "Mão de obra para substituição de display LED/IPS", new BigDecimal("130.00")),
            new ItemPadrao("Consultoria em TI (Hora Técnica)", "Atendimento remoto ou presencial para diagnóstico e suporte", new BigDecimal("95.00")),
            new ItemPadrao("Instalação de Câmera de Segurança", "Passagem de cabos, fixação e configuração no app do cliente", new BigDecimal("140.00")),
            new ItemPadrao("SSD 480GB SATA Kingston", "Unidade de estado sólido para upgrade de velocidade", new BigDecimal("210.00")),
            new ItemPadrao("Memória RAM 8GB DDR4 3200MHz", "Módulo de memória para desktop/notebook", new BigDecimal("165.00")),
            new ItemPadrao("Fonte ATX 500W 80 Plus Bronze", "Fonte de alimentação bivolt automática com PFC ativo", new BigDecimal("280.00")),
            new ItemPadrao("Cabo de Rede Cat6 (15m)", "Cabo de rede patch cord com conectores RJ45 blindados", new BigDecimal("55.00")),
            new ItemPadrao("Teclado e Mouse Sem Fio USB", "Kit periférico sem fio 2.4GHz ergonômico", new BigDecimal("99.90")),
            new ItemPadrao("Roteador Wi-Fi 6 Gigabit Dual Band", "Roteador de alta performance para residências e pequenos escritórios", new BigDecimal("340.00"))
    );

    /**
     * Lista todos os itens do catálogo do usuário. Se o catálogo estiver vazio,
     * popula automaticamente com os itens padrão de teste gerados para o usuário.
     */
    @Transactional
    public List<ItemCatalogoResponse> listar(String email) {
        Usuario usuario = buscarUsuario(email);
        List<ItemCatalogo> itens = catalogoRepository.findAllByUsuario_Id(usuario.getId());

        if (itens.isEmpty()) {
            popularItensPadrao(email);
            itens = catalogoRepository.findAllByUsuario_Id(usuario.getId());
        }

        return itens.stream()
                .map(i -> new ItemCatalogoResponse(i.getId(), i.getNome(), i.getDescricao(), i.getPreco()))
                .toList();
    }

    /**
     * Popula (ou repopula) os 12 itens padrão de teste no catálogo do usuário.
     */
    @Transactional
    public int popularItensPadrao(String email) {
        Usuario usuario = buscarUsuario(email);
        List<ItemCatalogo> novos = ITENS_PADRAO.stream()
                .map(p -> new ItemCatalogo(p.nome(), p.descricao(), p.preco(), usuario))
                .toList();
        catalogoRepository.saveAll(novos);
        return novos.size();
    }

    /**
     * Cria um novo item no catálogo do usuário.
     */
    @Transactional
    public ItemCatalogoResponse criarItem(String email, NovoItemRequest req) {
        Usuario usuario = buscarUsuario(email);
        ItemCatalogo item = new ItemCatalogo(
                req.nome().trim(),
                req.descricao() != null ? req.descricao().trim() : null,
                req.preco(),
                usuario
        );
        ItemCatalogo salvo = catalogoRepository.save(item);
        return new ItemCatalogoResponse(salvo.getId(), salvo.getNome(), salvo.getDescricao(), salvo.getPreco());
    }

    /**
     * Exclui um item do catálogo garantindo que pertence ao usuário autenticado.
     */
    @Transactional
    public void excluirItem(String email, UUID id) {
        Usuario usuario = buscarUsuario(email);
        ItemCatalogo item = catalogoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item não encontrado no catálogo."));
        if (!item.getUsuario().getId().equals(usuario.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado.");
        }
        catalogoRepository.delete(item);
    }

    @Transactional
    public int importarPlanilha(String email, MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O arquivo da planilha é obrigatório.");
        }

        Usuario usuario = buscarUsuario(email);
        List<ItemCatalogo> novosItens = new ArrayList<>();

        try (InputStream is = arquivo.getInputStream();
             Workbook workbook = WorkbookFactory.create(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A planilha não contém abas válidas.");
            }

            // Começa da linha 1 (linha 0 é o cabeçalho)
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                String nome = getValorTexto(row.getCell(0));
                if (nome == null || nome.isBlank()) continue;

                String descricao = getValorTexto(row.getCell(1));
                BigDecimal preco = getValorMonetario(row.getCell(2));

                if (preco == null || preco.compareTo(BigDecimal.ZERO) < 0) {
                    preco = BigDecimal.ZERO;
                }

                ItemCatalogo item = new ItemCatalogo(nome.trim(), descricao != null ? descricao.trim() : null, preco, usuario);
                novosItens.add(item);
            }

            if (novosItens.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nenhum item válido encontrado na planilha.");
            }

            catalogoRepository.saveAll(novosItens);
            return novosItens.size();

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erro ao importar planilha para o usuário: {}", email, e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Erro ao processar arquivo .xlsx: " + e.getMessage());
        }
    }

    private Usuario buscarUsuario(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão inválida."));
    }

    private String getValorTexto(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> null;
        };
    }

    private BigDecimal getValorMonetario(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            return BigDecimal.valueOf(cell.getNumericCellValue()).setScale(2, RoundingMode.HALF_UP);
        } else if (cell.getCellType() == CellType.STRING) {
            String str = cell.getStringCellValue().replaceAll("[^0-9,.-]", "").trim();
            if (str.isEmpty()) return null;
            if (str.contains(",") && !str.contains(".")) {
                str = str.replace(",", ".");
            } else if (str.contains(",") && str.contains(".")) {
                str = str.replace(".", "").replace(",", ".");
            }
            try {
                return new BigDecimal(str).setScale(2, RoundingMode.HALF_UP);
            } catch (Exception ignored) {}
        }
        return null;
    }
}
