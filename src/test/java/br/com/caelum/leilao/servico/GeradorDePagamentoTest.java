package br.com.caelum.leilao.servico;

import br.com.caelum.leilao.builder.CriadorDeLeilao;
import br.com.caelum.leilao.dominio.Leilao;
import br.com.caelum.leilao.dominio.Pagamento;
import br.com.caelum.leilao.dominio.Usuario;
import br.com.caelum.leilao.infra.relogio.Relogio;
import br.com.caelum.leilao.infra.relogio.RelogioDoSistema;
import br.com.caelum.leilao.repositorio.RepositorioDeLeiloes;
import br.com.caelum.leilao.repositorio.RepositorioDePagamentos;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Calendar;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GeradorDePagamentoTest {


    private RepositorioDeLeiloes leiloes;
    private RepositorioDePagamentos pagamentos;
    private Relogio relogio;


    @Before
    public void setup(){
        leiloes = mock(RepositorioDeLeiloes.class);
        pagamentos = mock(RepositorioDePagamentos.class);
        relogio = mock(Relogio.class);
    }

    @Test
    public void deveGerarPagamentoParaUmLeilaoEncerrado() {

        Avaliador avaliador = new Avaliador();

        Leilao leilao = new CriadorDeLeilao().para("Notebook")
                .lance(new Usuario("Jose"),2000.00)
                .lance(new Usuario("Maria"), 2500.00)
                .constroi();

        //Mockar os resultados
        when(leiloes.encerrados()).thenReturn(Arrays.asList(leilao));
        avaliador.avalia(leilao);

        GeradorDePagamento gerador = new GeradorDePagamento(leiloes,pagamentos,avaliador, new RelogioDoSistema());
        gerador.gera();

        //Capturador de pagamento do mock
        ArgumentCaptor<Pagamento> argumento = ArgumentCaptor.forClass(Pagamento.class);

        //Capturando o Pagamento que foi passago pelo método salva
        verify(pagamentos).salva(argumento.capture());

        Pagamento pagamentoGerado = argumento.getValue();

        assertEquals(2500.00,pagamentoGerado.getValor(),0.0001);

    }

    @Test
    public void deveEmpurrarParaOProximoDiaUtil() {

        Calendar sabado = Calendar.getInstance();
        sabado.set(2018,Calendar.MAY,12);

        RepositorioDeLeiloes leiloes = mock(RepositorioDeLeiloes.class);
        RepositorioDePagamentos pagamentos = mock(RepositorioDePagamentos.class);

        Leilao leilao = new CriadorDeLeilao()
                .para("Playstation")
                .lance(new Usuario("José da Silva"), 2000.0)
                .lance(new Usuario("Maria Pereira"), 2500.0)
                .constroi();

        when(leiloes.encerrados()).thenReturn(Arrays.asList(leilao));

        //Ensinar o mock a dizer que hoje eh sabado
        when(relogio.hoje()).thenReturn(sabado);

        GeradorDePagamento gerador =
                new GeradorDePagamento(leiloes, pagamentos, new Avaliador(), new RelogioDoSistema());
        gerador.gera();

        ArgumentCaptor<Pagamento> argumento = ArgumentCaptor.forClass(Pagamento.class);
        verify(pagamentos).salva(argumento.capture());
        Pagamento pagamentoGerado = argumento.getValue();

        assertEquals(14, pagamentoGerado.getData().get(Calendar.DAY_OF_MONTH));
        assertEquals(Calendar.MONDAY, pagamentoGerado.getData().get(Calendar.DAY_OF_WEEK));

    }
}
