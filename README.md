# Checkpoint 4 - Bug Hunt StreamFIAP

## Identificação

**Grupo:** Grupo 34 do Challenge

| Integrante | RM | Turma |
|---|---|---|
| Gabriel Fidalgo | 563213 | 2CCPY |
| Pedro Lima | 565461 | 2CCPY |
| Gustavo Maia | 562240 | 2CCPY |
| Gustavo Rossi | 566075 | 2CCPY |

| Campo | |
|---|---|
| Total de bugs corrigidos | 12 / 12 (+1 validação extra) |
| Total de ajustes de Clean Code | 6 / 6 (+2 extras) |

### Como rodar

1. Em `src/main/resources/application.properties`, troque `SEU_RM` e `SUA_SENHA` pelas credenciais do grupo. Essa alteração fica só na máquina, não vai para o commit.
2. No Eclipse: `File > Import > Maven > Existing Maven Projects`, depois play na `StreamFiapApplication`.
3. API sobe em `http://localhost:8080`.

Detalhe: o bug05 colocou `@GeneratedValue` no id do `Usuario`. Se o schema do grupo já tiver uma tabela `usuarios` criada antes disso (sem identity), apague a tabela antes de rodar, porque o `ddl-auto=update` não altera a estratégia de geração de uma coluna que já existe. O Hibernate recria na próxima execução.

---

## Parte 1 - Bugs encontrados

| # | Sintoma observado (o que fiz/vi) | Causa raiz (arquivo e linha aproximada) | Correção aplicada | Conceito da disciplina |
|---|---|---|---|---|
| bug01 | Mandei `POST /api/conteudos/filme` com `"duracaoMinutos":-5` e voltou 201, com o filme salvo no banco. O contrato diz para recusar. | `Conteudo.java:24`. O construtor só atribuía os campos, sem validar nada, e nenhuma subclasse validava também. | Coloquei as validações no construtor da superclasse (duração <= 0, título vazio, classificação negativa) lançando `IllegalArgumentException`, mais as mesmas checagens nos setters. No `GlobalExceptionHandler` adicionei o handler de `IllegalArgumentException` devolvendo 400 com a mensagem. Como está na superclasse, vale para filme, série e documentário de uma vez. | Encapsulamento, objeto válido desde a criação |
| bug02 | `GET /api/conteudos/999` respondia 200 com corpo vazio, como se tivesse achado. | `ConteudoController.java:32-39`. Tinha um `catch (Exception e)` vazio com `// TODO: tratar isso depois` engolindo a `ConteudoNaoEncontradoException`, e o método terminava em `return null`. | Tirei o try/catch. A exceção sobe até o `GlobalExceptionHandler`, que já tinha o handler pronto, e agora volta 404 com `{"erro":"Conteúdo não encontrado: 999"}`. | Tratamento de exceções |
| bug03 | Com dois conteúdos FICCAO cadastrados, `GET /api/conteudos/categoria/FICCAO` devolvia `[]`. | `ConteudoController.java:47`. O filtro era `if (c.getCategoria() == categoria)`, comparando referência de String em vez do conteúdo. De quebra, o `findByCategoria` do repository existia e nunca era chamado. | Troquei o loop manual por `conteudoRepository.findByCategoria(categoria)`. Resolve a comparação (a filtragem passa a ser no SQL) e usa o método que já estava lá. | `==` vs `equals`, query methods do Spring Data |
| bug04 | `POST /api/conteudos/serie` voltava 201, mas gravava `titulo:null`, `categoria:null`, `duracaoMinutos:0`, `classificacaoEtaria:0` e `disponivel:false`. Só o `numeroTemporadas` chegava certo. | `Serie.java:14-16`. O construtor não chamava `super(...)`, então rodava o construtor sem argumentos da `Conteudo` e todos os campos herdados ficavam no valor padrão. A assinatura ainda esquecia o `disponivel`. | Chamei `super(titulo, categoria, duracaoMinutos, classificacaoEtaria, disponivel)`, acrescentei o `disponivel` na assinatura e ajustei o `cadastrarSerie` do controller para passar `serie.isDisponivel()`. | Herança e cadeia de construtores |
| bug05 | Qualquer `POST /api/usuarios` voltava 500. Nenhum usuário era criado, o que travava todos os testes de aluguel. | `Usuario.java:11-12`. O `@Id private Long id` estava sem `@GeneratedValue`, então o JPA esperava um id atribuído na mão e recebia null. A `Conteudo` já usava `IDENTITY`. | Adicionei `@GeneratedValue(strategy = GenerationType.IDENTITY)`, igual à `Conteudo`. | Mapeamento JPA |
| bug06 | Depois que o cadastro passou a funcionar, o usuário voltava com `"nome":null`, tanto na resposta quanto no banco. | `Usuario.java:22`. O construtor tinha `nome = nome`, atribuindo o parâmetro a ele mesmo. O atributo nunca era tocado e o código compila normal. | `this.nome = nome`. | Escopo de variável e uso do `this` |
| bug07 | Série de 5 temporadas cobrava 9,90. Pelo contrato deveria ser 4,90 por temporada, ou seja 24,50. | `Serie.java:19`. O método era `calcularPrecoAluguel(double desconto)`, com assinatura diferente da superclasse. Isso é sobrecarga, não sobrescrita, e ninguém chamava esse método. O preço vinha do `Conteudo.calcularPrecoAluguel()`. | Tirei o parâmetro e coloquei `@Override public double calcularPrecoAluguel()` retornando `4.90 * numeroTemporadas`. | Sobrescrita vs sobrecarga, uso do `@Override` |
| bug08 | Documentário cobrava 9,90. O contrato diz gratuito. | `Documentario.java`. A classe não sobrescrevia `calcularPrecoAluguel()` e herdava o `return 9.90` de `Conteudo.java:33`. | `@Override public double calcularPrecoAluguel() { return 0.0; }`. | Polimorfismo |
| bug09 | `GET /api/conteudos/1/preco-promocional` num filme de 14,90 devolvia 17,88. A promoção aumentava o preço em 20% em vez de dar desconto. | `Filme.java:25`, `return preco * 1.2`. Contraria o Javadoc da `Promocionavel` e destoa da `Serie`, que já usava `0.8`. | `return preco * 0.8`. | Contrato de interface |
| bug10 | Consegui alugar um conteúdo com `disponivel:false`, e também alugar o mesmo conteúdo duas vezes seguidas. | `Usuario.java:36`, no `alugar`. A regra simplesmente não existia. A `ConteudoIndisponivelException` estava criada e com handler pronto, mas não era lançada em lugar nenhum. | Coloquei a checagem no começo do `alugar`: se `!conteudo.isDisponivel()`, lança `ConteudoIndisponivelException` com a mensagem "X nao esta disponivel para aluguel". Agora volta 409. | Regra de negócio no model, exceções de domínio |
| bug11 | Usuário de 12 anos tentando alugar conteúdo 14 anos recebia 500 genérico, sem explicação nenhuma. | `GlobalExceptionHandler.java`. Havia handler para as três exceções unchecked e nenhum para a `ClassificacaoIndicativaException`, que é checked e subia até o container. | Adicionei `@ExceptionHandler(ClassificacaoIndicativaException.class)` devolvendo 422 com a mensagem que o model já montava. | Checked vs unchecked, `@RestControllerAdvice` |
| bug12 | Usuário com 0 crédito alugava um filme de 14,90 e ficava com saldo -14,90. Usuário com 100 créditos era recusado por "créditos insuficientes". A lógica estava espelhada. | `Usuario.java:28`, `return preco >= this.creditos`. | Inverti para `return this.creditos >= preco` e coloquei a mesma checagem dentro do `debitarCreditos`, que agora recusa o débito se o saldo não cobrir. Assim o saldo não fica negativo nem se alguém chamar o débito direto. | Condição de guarda, invariante de estado |
| extra01 | Não está no contrato, mas o `POST /api/usuarios` aceitava nome vazio, idade negativa e créditos negativos. | `Usuario.java:21`, construtor sem validação. | Mesmas guardas do bug01, no construtor do `Usuario`. Volta 400. | Objeto válido desde a criação |

## Parte 2 - Ajustes de Clean Code

| # | Onde estava | Qual princípio/boas práticas era violado | O que eu mudei |
|---|---|---|---|
| clean01 | `Usuario.alugar(Conteudo c)` e a variável `double p`, em `Usuario.java:36` e `:43` | Nomes que não dizem nada. Para saber o que a variável guarda é preciso ler o método inteiro | `c` virou `conteudo` e `p` virou `preco` |
| clean02 | `ConteudoController.calcularDescontoAntigo`, linhas 88-94, com o comentário "mantido caso o time de marketing volte atrás" | Código morto. Método privado que ninguém chama, e ainda com uma regra de desconto que não é a do contrato | Apagado |
| clean03 | Bloco da regra de cupom comentado no fim do `ConteudoController`, linhas 96-100 | Código comentado para reativar depois. Vira ruído e o histórico é papel do Git | Apagado |
| clean04 | `public int duracaoMinutos` em `Conteudo.java:16`, acessado direto como `filme.duracaoMinutos` nos três POSTs do controller | Quebra de encapsulamento. Campo público pode ser alterado de qualquer lugar, passando por cima da validação do setter | Campo virou `private` e o controller passou a usar `getDuracaoMinutos()` |
| clean05 | `// adiciona o valor aos créditos do usuário` em cima de um método que subtrai (`Usuario.java:32`), `// TODO: tratar isso depois` no catch vazio, `// cria a série com os dados recebidos` em cima do construtor que perdia os dados, e comentários que só repetiam a annotation, como `// GET /api/conteudos - Listar todos` | Comentário que mente é pior que comentário nenhum. Comentário que repete o código é ruído | Os comentários errados saíram junto com os bugs que eles descreviam mal (bug04 e bug12). Os redundantes foram removidos. Ficaram só os que explicam o porquê, como o do controller que recria a instância para ignorar um id vindo do cliente |
| clean06 | `Usuario.alugar`, linhas 52-59, com oito `System.out.println` do recibo no meio da regra | Método com duas responsabilidades. Decidia o aluguel e formatava a impressão | Extraí `private void imprimirRecibo(Conteudo, double)`. O `alugar` ficou só com a regra |
| clean07 (extra) | O mesmo `orElseThrow(() -> new ConteudoNaoEncontradoException(...))` repetido no `buscarPorId` e no `precoPromocional` | Repetição | Extraí `private Conteudo buscarConteudoOuFalhar(Long id)` |
| clean08 (extra) | `Conteudo.calcularPrecoAluguel()` com `return 9.90`, linhas 32-34 | A classe abstrata tinha o preço do filme chumbado. Era justamente esse retorno que escondia os bugs 07 e 08: quem esquecesse o override cobrava preço de filme sem ninguém perceber | O método virou `abstract`. Agora o compilador obriga cada subclasse a declarar o próprio preço |

---

## Parte 3 - Perguntas de reflexão

### 1. Injeção de dependência (Aula 13)

`new ConteudoRepository()` nem compila, porque `ConteudoRepository` é uma interface e não tem nenhuma classe nossa implementando ela. Quem faz o objeto é o Spring Data. Na hora que a aplicação sobe ele gera um proxy que implementa a interface, transforma cada método (`findById`, `findByCategoria`) em JPQL e deixa isso registrado como bean. O `@Autowired` do `ConteudoController` só pega esse bean pronto.

Mesmo se a classe existisse, dar `new` ia ser ruim. O repository precisa do `EntityManager`, que precisa do `DataSource`, que precisa das credenciais do `application.properties`. Ia sobrar pra gente montar essa corrente inteira na mão dentro do controller. Além disso o `ConteudoController` e o `AluguelController` recebem a mesma instância, com o mesmo pool de conexão e a mesma transação. Com `new` seriam dois objetos separados sem nada disso.

### 2. JDBC vs Spring Data JPA (Aulas 12 e 13)

No `ProdutoDAO` da Aula 12 a gente fazia tudo na mão: abria a `Connection`, montava o `PreparedStatement`, setava cada `?` na posição certa, percorria o `ResultSet` campo por campo e fechava tudo no `finally`. Aqui o `ConteudoRepository` tem duas linhas e já veio com `save`, `findById`, `findAll` e `deleteById`, porque isso tudo vem do `JpaRepository<Conteudo, Long>` e o Hibernate monta o SQL a partir das annotations (`@Entity`, `@Table(name="conteudos")`, `@GeneratedValue`).

O `findByCategoria` funciona sem implementação porque o Spring Data lê o nome do método. `findBy` + `Categoria` vira um `where c.categoria = ?1`. Isso é prático mas tem um custo: o nome do atributo virou parte do contrato, se alguém renomear o campo `categoria` o método quebra.

O JDBC continua melhor quando a consulta é pesada, quando precisa de SQL específico do banco, insert em lote ou um relatório com vários joins. Nessas horas é melhor escrever o SQL e saber exatamente o que está indo pro banco.

### 3. Exceções checked vs unchecked (Aula 11)

`ClassificacaoIndicativaException extends Exception`, então é checked e o compilador obriga a tratar ou declarar. É por isso que `Usuario.alugar` e `AluguelController.alugar` têm `throws` na assinatura. As outras três estendem `RuntimeException`, são unchecked e sobem sozinhas.

Mas o bug não foi o tipo. Foi esquecimento mesmo: o `GlobalExceptionHandler` tinha handler pras três unchecked e nenhum pra checked. Sem handler o Spring cai no tratamento padrão, devolve 500 com corpo genérico e a mensagem da regra morre no caminho. Registramos `@ExceptionHandler(ClassificacaoIndicativaException.class)` devolvendo 422 com `{"erro": e.getMessage()}` e a mensagem que o model já montava passou a chegar no cliente.

Pensando bem, classificação indicativa é violação de regra de negócio, não é algo que quem chamou consiga contornar. Se ela fosse unchecked igual as outras, o `throws` sumia de duas assinaturas e o resultado seria o mesmo.

### 4. Sobrescrita vs sobrecarga (Aula 7)

A `Serie` tinha `calcularPrecoAluguel(double desconto)` e a superclasse tem `calcularPrecoAluguel()`, sem parâmetro. Assinatura diferente quer dizer método diferente, então aquilo era sobrecarga e não sobrescrita.

Como o resto do código chama sempre `conteudo.calcularPrecoAluguel()`, o polimorfismo pegava a versão da `Conteudo`, que devolvia 9,90 fixo. Resultado: série de 5 temporadas cobrando preço de filme e o método da `Serie` sem nunca ser executado. E não tem erro de compilação nenhum nisso, ter um método a mais é totalmente válido em Java.

Com `@Override` em cima daquela assinatura o compilador reclamaria na hora ("method does not override or implement a method from a supertype"). Foi o que nos levou ao clean08: deixamos `calcularPrecoAluguel()` abstrata, aí esquecer o override virou erro de compilação em vez de preço errado rodando em produção.

### 5. Onde blindar o objeto? (Aulas 3, 4 e 13)

No construtor entram as regras do que o objeto precisa ter pra existir. Conteúdo sem título ou com duração zero não deveria nem ser criado (bug01), e usuário sem nome ou com crédito negativo também não (extra01).

No setter entra a mesma coisa, porque o construtor só protege a criação. Um `setDuracaoMinutos(-5)` depois deixava o objeto inválido de novo, então tiramos as checagens pros métodos `validarTitulo` e `validarDuracao` e usamos nos dois lugares.

No método de negócio ficam as regras que dependem do estado atual e de outro objeto. `alugar` olha disponibilidade, idade e saldo. `debitarCreditos` recusa débito que deixaria o saldo negativo (bug12).

Validar em um lugar só não segura. Se a checagem de crédito ficasse no controller, qualquer outro código que chamasse `usuario.alugar(...)` passaria por cima. E se ficasse só dentro do `alugar`, um `debitarCreditos` chamado direto ainda zerava o saldo do cara. Cada camada defende o que é dela: o controller cuida do HTTP, o model cuida das próprias invariantes e o banco (`not null`, `check`) é a última barreira.

### 6. Abstração e interface (Aulas 8 e 9)

`Conteudo` é abstrata e diz o que todo conteúdo é: id, título, categoria, duração, classificação, disponibilidade. É a raiz da herança que o JPA joga numa tabela só. `Promocionavel` é interface e diz o que alguns conteúdos sabem fazer, sem obrigar estado nem posição na hierarquia. As duas coisas são separadas porque promoção é opcional e não tem relação com o tipo do conteúdo: filme e série implementam, documentário não.

Se o documentário passasse a ter promoção, mudaria uma linha e meia. `class Documentario extends Conteudo implements Promocionavel` e o método `aplicarPromocao(preco)`. Nada além disso seria tocado, porque `calcularPrecoPromocional` já pergunta `this instanceof Promocionavel` e nem os controllers nem os repositories sabem que promoção existe. Regra nova custando uma classe alterada e nenhum `if` novo é sinal de que o desenho está razoável.

O ponto fraco é justamente esse `instanceof` com cast dentro da `Conteudo`, que é polimorfismo feito na mão e ainda faz a superclasse conhecer a interface. Daria pra `calcularPrecoPromocional()` devolver o preço cheio por padrão e quem tem promoção sobrescrever.

---

## Parte 4 - Espaço livre

Como a gente testou: o projeto não tem teste automatizado e o `pom.xml` não pode ganhar dependência nova, então montamos um roteiro de `curl` passando por cada linha da tabela do contrato. Os quatro preços (filme 9,90, estreia 14,90, série 4,90 por temporada, documentário 0,00), os promocionais (11,92 e 19,60), duração inválida, `GET /conteudos/999`, busca por categoria, cadastro de usuário e os quatro cenários de aluguel. Aproveitamos e testamos alguns casos de borda que não estão na tabela: alugar o mesmo conteúdo duas vezes, alugar documentário gratuito com 0 crédito, e usuário ou conteúdo que não existe.

A parte mais chata foram os bugs em cascata. Enquanto o `POST /api/usuarios` estava dando 500 (bug05) não dava pra criar ninguém, e sem usuário nenhum teste de aluguel roda. Ou seja, quatro bugs ficaram invisíveis até esse ser corrigido. O `return 9.90` da superclasse fazia parecido com os bugs 07 e 08, os dois escondidos atrás do mesmo valor.

Uma coisa que a gente viu e decidiu não mexer: como preço é `double`, aparece a imprecisão binária de sempre. O promocional do filme sai `11.920000000000002` e o saldo depois de alugar a série fica `60.599999999999994`. O valor está certo, só a representação que é aproximada. O contrato não fala de arredondamento e trocar o tipo mexeria no mapeamento JPA, então deixamos como está. Num sistema de verdade dinheiro seria `BigDecimal`.
