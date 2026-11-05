package com.autorecibo.api.application.service;

import com.autorecibo.api.domain.model.ItemCatalogo;
import com.autorecibo.api.domain.model.Usuario;
import com.autorecibo.api.infrastructure.persistence.repository.ItemCatalogoRepository;
import com.autorecibo.api.infrastructure.persistence.repository.UsuarioRepository;
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

    @Transactional(readOnly = true)
    public List<ItemCatalogoResponse> listar(String email) {
        Usuario usuario = buscarUsuario(email);
        return catalogoRepository.findAllByUsuario_Id(usuario.getId()).stream()
                .map(i -> new ItemCatalogoResponse(i.getId(), i.getNome(), i.getDescricao(), i.getPreco()))
                .toList();
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
