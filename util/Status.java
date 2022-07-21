/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util;

/**
 *
 * @author elder
 */
public enum Status {
   OK, 

   /*
    * se estiver em:
    VOTOCONF: candidato inexistente
    CANDRESP: nao aceito
    COUNTCONF: inconsistência na contagem dos votos, só manda contagem antiga para "travar" votações
    */
   ERROR, 
   
   /*
    * se estiver em:
    VOTOCONF: titulo incorreto
    AUTHRESP: informações incorretas
    CANDCONF: nr candidato ocupado
    */
   PARAMERROR,

   NOAUTH
}
