package services;

import com.sun.corba.se.spi.ior.ObjectKey;
import jadex.extension.envsupport.math.Vector2Int;

public interface ICommunicationService {
    /*
        Mensagem enviada pelo agente pessoa quando o mesmo se sentir ameaçado pelo fogo.
        O agente pessoa envia ao agente Bombeiro mais próximo dele. Caso o bombeiro responda afirmativamente, o mesmo fica
        responsável pelo salvamento do agente pessoa.
        
        Caso a resposta seja negativa, o agente tenta o proximo agente, até que um responda afirmativa.
        
        Caso nenhum possa de momento, o agente repete este método de segundo a segundo.
    
    */
    public void RescueMessage(Object senderID, Object receiverID);
    
    /*
        O agente bombeiro após receber uma mensagem de salvamento, ele responde a confirmar caso esteja disponivel
        ( em nenhuma missão de salvamento ).
        
        Ao enviar isto, o agente começa a deslocar-se em direção da pessoa, até a encontrar ou ela morrer.
    */
    public void RescueConfirmation(Object firemanID, Object personID);

    /*
        Caso o agente bombeiro esteja numa missão de salvamento, o bombeiro responde negativamento ao pedido de forma a que
        o agente pessoa consiga arranjar outro salvador mais rápido - sem esperar o segundo.
    */
    public void refuseRescueRequest(Object firemanID, Object personID);
    
    /*
        Caso o agente bombeiro tenha adoptado o comportamento de Quadrantes, o lider do grupo de bombeiros previamente
        definido, envia aos outros bombeiros que quandrante lhes foi atribuido.
    */
    public void sendQuad(Object leaderID, Object firemanID, int quad);
    
    /*
        Um bombeiro não lider, ao receber um quadrante por parte do lider, envia uma confirmação, não que possa recusar, mas 
        para que o bombeiro lider saiba que todos os bombeiros estão vivos ou existem.
    */
    public void sendConfirmationQuad(Object leaderID, Object firemanID,int quad);
}
