Eleição Peer-to-Peer
==================

Aplicação simples desenvolvida para a disciplina de Sistemas Distribuídos I no curso de Ciência da Computação do Instituto Federal Sul-Riograndense (IFSUL) campus Passo Fundo baseada no seguinte enunciado:

## Programação de Middleware de SD

A comunicação entre processos é um requisito fundamental para a integração de
sistemas. Integrar sistemas é uma necessidade em diversas realidades em sistemas
contemporâneos: Web, IoT, BDs distribuídos, nuvens, entre tantos. A pilha TCP/IP é a base
tudo isto. Embora já existam vários frameworks que atuem como middlewares de
comunicação, todos utilizam das mesmas bases. Portanto é fundamental a um cientista da
computação compreender, projetar e ser capaz de aplicar modelos de comunicação dentro
da pilha TCP/IP.
O objetivo deste trabalho é justamente este: torná-lo capaz de desenvolver
middlewares e aplicações distribuídas.

## Proposta de aplicação

A Sbórnia está em alvoroço! As eleições se aproximam e a gloriosa nação não conta
com um sistema eleitoral que funcione. Com a morte do Supremo Imperador, resolveram
inventar a democracia na Nação, porém, às vésperas das eleições, deram-se de conta que
não possuíam um sistema eleitoral. Como os cidadãos esbornianos estão espalhados pelo
mundo, programar este sistema de forma distribuída e segura é de fundamental importância
para o bem-geral-da-nação e você, nobre desenvolvedor, será bem recompensado.

>A referência à Sbórnia é uma singela homenagem à dupla de artistas gaúchos autores da peça de teatro Tangos e Tragédias, a qual conta a saga do continente itinerante era um istmo e que um dia se desgrudou; e hoje vaga pelos mares do mundo, mares do mundo...

### 1. Inscrever candidatos
a. Antes de as eleições começarem, o peer coordenador pode receberinscrições de candidatos dos demais peers. Essa inscrição deve conter qual o candidato que está se inscrevendo e qual o seu número;
b. devem ser restrito para impedir candidaturas com nomes ou números iguais;

### 2. Iniciar e finalizar eleições
a. O peer coordenador deve enviar uma mensagem iniciando e finalizando a eleição. Quando iniciada, não pode mais receber inscrições de candidatos. A votação só poderá ocorrer entre o início e o final das eleições.
b. A finalização da eleição deve enviar o resultado desta para todos os peers;

### 3. Cadastrar eleitores:
a. somente o coordenador pode cadastrar eleitores
b. O cadastro deve ser feito enviando o nome e o número do título do eleitor. Um por vez ou em blocos de vários eleitores;

### 4. Votar
a. A votação deve ser permitida em todos os peers autenticados (opcional).
b. O “eleitor” deve informar seu título eleitoral cadastrado pelo coordenador
  - feito isso, ele deve receber como resposta a lista de candidatos seus respectivos números;
  - o voto deve ser informando o número do candidato;
  - cada eleitor pode votar somente uma vez;
  - os votos devem ser armazenados localmente no peer o qual o eleitor votou e no coordenador do sistema.

### 5. Verificação da apuração
a. Aproveitando que a aplicação tem a possibilidade de ser peer-to-peer, deve ser implementado um sistema de apuração distribuído para confirmar os resultados das eleições.
b. O sistema deve permitir que qualquer peer solicite essa verificação. Ao solicitar, todos os peers devem enviar os votos realizados localmente para que o peer solicitante possa contar e conferir com o resultado oficial.
  - Deve ser exibida informação sobre a validade da votação e, em caso de possível fraude, todos os peers devem ser alertados.
c. (funcionalidade EXTRA +1): usar o sistema de estado global do JGroups para armazenar localmente todos os votos. Assim é possível que cada peer possa confirmar o resultado das eleições comparando com os resultados armazenados nos demais. Se houver divergências, pode-se resolver.

### 6. Autenticação dos peers (EXTRA +1)
a. Desenvolver uma forma de autenticar cada peer que entrar no canal. Somente os peers autenticados poderão realizar qualquer operação. Esta autenticação também deve permitir que um peer autenticado qualquer verifique a validade de outro peer.
