package util;

import java.io.Serializable;

import org.jgroups.Address;

public class Eleitor implements Serializable{
    
    private long titulo;
    private String nome;
    transient private boolean inscrito = false;
    transient private boolean votou = false;
    transient private Address endereco;

    public Eleitor(){

    }

    public Eleitor(long titulo, String nome){
        this.titulo = titulo;
        this.nome = nome;
    }

    public long getTitulo(){
        return this.titulo;
    }

    public void setTitulo(long titulo){
        this.titulo = titulo;
    }

    public String getNome(){
        return this.nome;
    }

    public void setNome(String nome){
        this.nome = nome;
    }

    public Address getEndereco(){
        return this.endereco;
    }

    public void setEndereco(Address endereco){
        this.endereco = endereco;
    }

    public boolean isInscrito(){
        return inscrito;
    }

    public boolean votou(){
        return votou;
    }

    public void inscrever(){
        inscrito = true;
    }
}
