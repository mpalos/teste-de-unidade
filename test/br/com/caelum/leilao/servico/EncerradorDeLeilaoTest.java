package br.com.caelum.leilao.servico;

import br.com.caelum.leilao.builder.CriadorDeLeilao;
import br.com.caelum.leilao.dominio.Leilao;
import br.com.caelum.leilao.repositorio.RepositorioDeLeiloes;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class EncerradorDeLeilaoTest {

    //Mockando o dao
    private RepositorioDeLeiloes leilaoDaoMock;
    private EnviadorDeEmail carteiroMock;
    private EncerradorDeLeilao encerrador;
    private Calendar antiga, ontem;

    @Before
    public void setup(){
        leilaoDaoMock = mock(RepositorioDeLeiloes.class);
        carteiroMock = mock(EnviadorDeEmail.class);
        encerrador = new EncerradorDeLeilao(leilaoDaoMock, carteiroMock);

        antiga = Calendar.getInstance();
        antiga.set(1999,1,20);

        ontem = Calendar.getInstance();
        ontem.set(2018,4,22);

    }

    @Test
    public void deveEncerrarLeiloesQueComecaramUmaSemanaAtras(){
        Leilao leilao1 = new CriadorDeLeilao().para("Televisão").naData(antiga).constroi();
        Leilao leilao2 = new CriadorDeLeilao().para("Geladeira").naData(antiga).constroi();

        // Lista que é o resultado da invocação do método mockado
        List<Leilao> leiloesAntigos = Arrays.asList(leilao1,leilao2);

        //Ensinando o mock a devolver uma lista quando o método correntes é invocado
        when(leilaoDaoMock.correntes()).thenReturn(leiloesAntigos);

        EncerradorDeLeilao encerrador = new EncerradorDeLeilao(leilaoDaoMock, carteiroMock);
        encerrador.encerra();

        assertEquals(2,encerrador.getTotalEncerrados());
        assertTrue(leilao1.isEncerrado());
        assertTrue(leilao2.isEncerrado());

    }

    @Test
    public void naoDeveEncerrarLeiloesQueComecaramMenosDeUmaSemanaAtras(){

        Leilao leilao1 = new CriadorDeLeilao().para("Item 1").naData(ontem).constroi();
        Leilao leilao2 = new CriadorDeLeilao().para("Item 2").naData(ontem).constroi();

        // Lista que é o resultado da invocação do método mockado
        List<Leilao> leiloesRecentes = Arrays.asList(leilao1,leilao2);

        //Ensinando o mock a devolver uma lista quando o método correntes é invocado
        when(leilaoDaoMock.correntes()).thenReturn(leiloesRecentes);

        encerrador.encerra();

        assertEquals(0,encerrador.getTotalEncerrados());
        assertFalse(leilao1.isEncerrado());
        assertFalse(leilao2.isEncerrado());

    }

    @Test
    public void deveIgnorarLeiloesVazios(){

       when(leilaoDaoMock.correntes()).thenReturn(new ArrayList<Leilao>());

       encerrador.encerra();

       assertEquals(0,encerrador.getTotalEncerrados());

    }

    @Test
    public void deveAtualizarLeiloesEncerrados(){

        Leilao leilao1 = new CriadorDeLeilao().para("Geladeira").naData(antiga).constroi();
        Leilao leilao2 = new CriadorDeLeilao().para("Mesa").naData(antiga).constroi();

        // Lista que é o resultado da invocação do método mockado
        List<Leilao> leiloesAntigos = Arrays.asList(leilao1);

        //Ensinando o mock a devolver uma lista quando o método correntes é invocado
        when(leilaoDaoMock.correntes()).thenReturn(leiloesAntigos);

        encerrador.encerra();

        //Verificando se o método foi invocado
        verify(leilaoDaoMock).atualiza(leilao1);

        //Verificando se o método foi invocado 1 X
        verify(leilaoDaoMock,times(1)).atualiza(leilao1);

        //ou
        verify(leilaoDaoMock,atLeastOnce()).atualiza(leilao1);

        //Verificando se o método não foi invocado
        verify(leilaoDaoMock,never()).atualiza(leilao2);

    }

    @Test
    public void deveEnviarEmailDepoisDePersistirNoDAO(){

        Leilao leilao1 = new CriadorDeLeilao().para("Geladeira").naData(antiga).constroi();

        // Lista que é o resultado da invocação do método mockado
        List<Leilao> leiloesAntigos = Arrays.asList(leilao1);

        //Ensinando o mock a devolver uma lista quando o método correntes é invocado
        when(leilaoDaoMock.correntes()).thenReturn(leiloesAntigos);

        encerrador.encerra();

        // passamos os mocks que serao verificados
        InOrder inOrder = Mockito.inOrder(leilaoDaoMock,carteiroMock);

        inOrder.verify(leilaoDaoMock, atLeastOnce()).atualiza(leilao1);
        inOrder.verify(carteiroMock, atLeastOnce()).envia(leilao1);

    }

    @Test
    public void deveContinuarAExecucaoMesmoQuandoDaoFalha(){

        Leilao leilao1 = new CriadorDeLeilao().para("TV de plasma").naData(antiga).constroi();
        Leilao leilao2 = new CriadorDeLeilao().para("Geladeira").naData(antiga).constroi();

        when(leilaoDaoMock.correntes()).thenReturn(Arrays.asList(leilao1, leilao2));
        doThrow(new RuntimeException()).when(leilaoDaoMock).atualiza(leilao1);

        encerrador.encerra();

        verify(carteiroMock, never()).envia(leilao1);

        verify(leilaoDaoMock).atualiza(leilao2);
        verify(carteiroMock).envia(leilao2);

    }

    @Test
    public void deveContinuarAExecucaoMesmoQuandoEnviadorDeEmailFalha(){

        Leilao leilao1 = new CriadorDeLeilao().para("TV de plasma").naData(antiga).constroi();
        Leilao leilao2 = new CriadorDeLeilao().para("Geladeira").naData(antiga).constroi();

        when(leilaoDaoMock.correntes()).thenReturn(Arrays.asList(leilao1, leilao2));
        doThrow(new RuntimeException()).when(carteiroMock).envia(leilao1);

        encerrador.encerra();

        verify(leilaoDaoMock,atLeastOnce()).atualiza(leilao1);

        verify(leilaoDaoMock).atualiza(leilao2);
        verify(carteiroMock).envia(leilao2);

    }

    @Test
    public void naoDeveContinuarAExecucaoQuandoEnviadorDeEmailFalha(){

        Leilao leilao1 = new CriadorDeLeilao().para("TV de plasma").naData(antiga).constroi();
        Leilao leilao2 = new CriadorDeLeilao().para("Geladeira").naData(antiga).constroi();

        when(leilaoDaoMock.correntes()).thenReturn(Arrays.asList(leilao1, leilao2));
        doThrow(new RuntimeException()).when(leilaoDaoMock).atualiza(any(Leilao.class));

        encerrador.encerra();

        verify(carteiroMock, never()).envia(any(Leilao.class));
    }

}
