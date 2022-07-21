package util;

import java.io.Serializable;
import java.util.ArrayDeque;

import org.jgroups.Address;

public class Candidato implements Serializable {

    private String nome;
    private Integer numero;
    private Integer votos = 0;
    transient boolean confirmado = false;
    transient Address endereco;

    public Candidato(){

    }

    public Candidato(Integer numero, String nome){
        this.numero = numero;
        this.nome = nome;
    }

    public String getNome(){
        return nome;
    }

    public void setNome(String nome){
        this.nome = nome;
    }

    public Integer getNumero(){
        return numero;
    }

    public void setNumero(Integer numero){
        this.numero = numero;
    }

    public Integer getVotos(){
        return votos;
    }

    public void setVotos(Integer votos){
        this.votos = votos;
    }

    public Address getEndereco(){
        return endereco;
    }

    public void setEndereco(Address endereco){
        this.endereco = endereco;
    }

    public void confirma(){
        this.confirmado = true;
    }

    public void vota(){
        this.votos++;
    }

    public String toString(){
        String ret = "";
        ret += nome + "\t";
        ret += numero + "\t";
        ret += confirmado + "\t";
        ret += votos + "\t";
        return ret;
    }

    public boolean equals(Candidato c){
        return c.getNome().equals(nome) &&
            c.getVotos().equals(votos) &&
            c.getNumero().equals(numero);
    }
}
