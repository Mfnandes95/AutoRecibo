package com.autorecibo.api.application.service;

import com.autorecibo.api.domain.model.Documento;
import com.autorecibo.api.domain.model.ItemDocumento;
import com.autorecibo.api.domain.model.TipoDocumento;
import com.autorecibo.api.domain.model.Usuario;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfService {

    private final TemplateEngine templateEngine;

    private static final Locale PT_BR = Locale.of("pt", "BR");
    private static final DateTimeFormatter DATA_HORA_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public record ItemPdfDto(
            int ordem,
            String nome,
            String quantidade,
            String precoUnitario,
            String desconto,
            String valorLiquido
    ) {}

    public byte[] gerarPdf(Documento doc, Usuario emitente) {
        try {
            Context context = new Context(PT_BR);

            boolean isOs = doc.getTipo() == TipoDocumento.ORDEM_SERVICO;
            String tipoTitulo = isOs ? "ORDEM DE SERVIÇO" : "RECIBO DE PAGAMENTO";
            String numeroFormatado = String.format("%05d", doc.getNumero() != null ? doc.getNumero() : 0);

            context.setVariable("tipoTitulo", tipoTitulo);
            context.setVariable("isOrdemServico", isOs);
            context.setVariable("numero", numeroFormatado);
            context.setVariable("dataEmissao", doc.getEmitidoEm() != null ? doc.getEmitidoEm().format(DATA_HORA_FORMATTER) : "-");

            // Dados do Emissor (MEI)
            context.setVariable("emitenteEmail", emitente.getEmail());
            context.setVariable("emitenteDocumento", formatarDocumento(emitente.getDocumento()));

            // Dados do Cliente
            context.setVariable("clienteNome", doc.getClienteNome());
            context.setVariable("clienteDocumento", formatarDocumento(doc.getClienteDocumento()));
            context.setVariable("temClienteDocumento", doc.getClienteDocumento() != null && !doc.getClienteDocumento().isBlank());

            // Observações
            context.setVariable("observacoes", doc.getObservacoes());
            context.setVariable("temObservacoes", doc.getObservacoes() != null && !doc.getObservacoes().isBlank());

            // Itens
            List<ItemPdfDto> itensPdf = new ArrayList<>();
            if (doc.getItens() != null) {
                for (ItemDocumento item : doc.getItens()) {
                    String descPct = item.getDescontoPercentual() > 0 ? item.getDescontoPercentual() + "%" : "-";
                    itensPdf.add(new ItemPdfDto(
                            item.getOrdem() + 1,
                            item.getNome(),
                            formatarQuantidade(item.getQuantidade()),
                            formatarMoeda(item.getPrecoUnitario()),
                            descPct,
                            formatarMoeda(item.getValorLiquido())
                    ));
                }
            }
            context.setVariable("itens", itensPdf);

            // Totais
            context.setVariable("subtotal", formatarMoeda(doc.getSubtotal()));
            context.setVariable("totalDescontos", formatarMoeda(doc.getTotalDescontos()));
            context.setVariable("total", formatarMoeda(doc.getTotal()));

            // Renderiza o HTML via Thymeleaf
            String html = templateEngine.process("documento-pdf", context);

            // Gera o PDF via OpenHTMLToPDF (PDFBox)
            try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
                PdfRendererBuilder builder = new PdfRendererBuilder();
                builder.useFastMode();
                builder.withHtmlContent(html, null);
                builder.toStream(os);
                builder.run();
                return os.toByteArray();
            }

        } catch (Exception e) {
            log.error("Erro ao gerar PDF para o documento ID: {}", doc.getId(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Falha na geração do arquivo PDF: " + e.getMessage());
        }
    }

    private String formatarDocumento(String doc) {
        if (doc == null || doc.isBlank()) return "-";
        String d = doc.replaceAll("\\D", "");
        if (d.length() == 11) {
            return d.substring(0, 3) + "." + d.substring(3, 6) + "." + d.substring(6, 9) + "-" + d.substring(9);
        } else if (d.length() == 14) {
            return d.substring(0, 2) + "." + d.substring(2, 5) + "." + d.substring(5, 8) + "/" + d.substring(8, 12) + "-" + d.substring(12);
        }
        return doc;
    }

    private String formatarMoeda(BigDecimal valor) {
        if (valor == null) return "R$ 0,00";
        NumberFormat nf = NumberFormat.getCurrencyInstance(PT_BR);
        return nf.format(valor);
    }

    private String formatarQuantidade(BigDecimal qtd) {
        if (qtd == null) return "0";
        DecimalFormat df = new DecimalFormat("#,##0.###", new DecimalFormatSymbols(PT_BR));
        return df.format(qtd);
    }
}
