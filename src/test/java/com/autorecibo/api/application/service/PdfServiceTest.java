package com.autorecibo.api.application.service;

import com.autorecibo.api.domain.model.Documento;
import com.autorecibo.api.domain.model.ItemDocumento;
import com.autorecibo.api.domain.model.TipoDocumento;
import com.autorecibo.api.domain.model.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PdfServiceTest {

    @Autowired
    private PdfService pdfService;

    @Test
    @DisplayName("Deve gerar PDF com cabeçalho %PDF válido para um documento")
    void deveGerarPdfComSucesso() {
        Usuario emitente = new Usuario();
        emitente.setId(UUID.randomUUID());
        emitente.setEmail("mei@exemplo.com");
        emitente.setDocumento("12345678000195");

        Documento doc = new Documento();
        doc.setId(UUID.randomUUID());
        doc.setNumero(1L);
        doc.setTipo(TipoDocumento.RECIBO);
        doc.setClienteNome("Cliente Teste");
        doc.setClienteDocumento("12345678901");
        doc.setObservacoes("Pagamento referente a serviços de TI.");
        doc.setEmitidoEm(LocalDateTime.now());
        doc.setSubtotal(new BigDecimal("100.00"));
        doc.setTotalDescontos(new BigDecimal("10.00"));
        doc.setTotal(new BigDecimal("90.00"));
        doc.setItens(new ArrayList<>());

        ItemDocumento item = new ItemDocumento();
        item.setOrdem(0);
        item.setNome("Consultoria Técnica");
        item.setQuantidade(new BigDecimal("1"));
        item.setPrecoUnitario(new BigDecimal("100.00"));
        item.setDescontoPercentual(10);
        item.setValorBruto(new BigDecimal("100.00"));
        item.setValorDesconto(new BigDecimal("10.00"));
        item.setValorLiquido(new BigDecimal("90.00"));
        doc.getItens().add(item);

        byte[] pdfBytes = pdfService.gerarPdf(doc, emitente);

        assertNotNull(pdfBytes, "O array de bytes do PDF não deve ser nulo");
        assertTrue(pdfBytes.length > 100, "O PDF gerado deve ter tamanho substancial");

        // Valida cabeçalho padrão de arquivo PDF (%PDF-)
        String header = new String(pdfBytes, 0, Math.min(pdfBytes.length, 5));
        assertEquals("%PDF-", header, "O arquivo gerado deve iniciar com %PDF-");
    }
}
