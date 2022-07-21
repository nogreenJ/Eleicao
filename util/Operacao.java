package util;

public enum Operacao {
    //Vota: nr candidato
    VOTO, 

    //Confirma voto: status
    VOTOCONF,

    //Autentica: nome, titulo eleitor
    AUTH, 

    //Confirma q autenticou: status
    AUTHRESP,

    //Pedido de se candidatar: numero que quer, nome
    CAND, 

    //pede para mostrar candidatos
    SHOWCAND,

    //fala q recebeu: status
    CANDCONF, 

    //fala se confirmou ou negou, status, BROAD SE SIM
    CANDRESP,

    //Inicia eleicao, termina eleicao, aborta (deleta tudo e reinicia)
    START, END, ABORT,

    //ABAIXO APURAÇÃO PÓS ELEIÇÃO

    //peer pede recontagem ->
    COUNT,

    //coordenador manda broadcast ou fala pra esperar eleicao acabar ->
    COUNTCONF,

    //peers respondem se ta tudo bem ->
    COUNTRESP,

    //coordenador confirma estar tudo bem
    COUNTRESPCONF
}
