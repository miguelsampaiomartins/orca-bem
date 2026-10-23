# OrçaBem

Aplicativo desktop de orçamento pessoal, feito em Java 17 e Swing. O OrçaBem reúne lançamentos, categorias, orçamentos e metas em um painel simples, com identidade própria em verde-floresta, marfim e âmbar. Os dados ficam no próprio computador; não há conta, servidor ou banco em nuvem.

## O que faz

- Registra receitas e despesas com data, categoria, descrição opcional e valor.
- Permite editar ou excluir um lançamento selecionado e pesquisar por descrição ou categoria.
- Apresenta quanto sobra depois dos gastos, receitas, despesas e orçamento ainda disponível no mês selecionado.
- Mostra a distribuição percentual dos gastos por categoria e o fluxo de caixa de receitas e despesas nos últimos seis meses ou em todo o período registrado.
- Permite definir limites mensais por categoria e comparar o limite com o gasto realizado, com alertas em linguagem simples quando está perto ou foi ultrapassado.
- Permite adicionar e remover categorias de receitas e despesas. Lançamentos antigos e orçamentos salvos são preservados quando uma categoria sai da lista.
- Inclui uma meta de economia por mês com uma barra de progresso baseada no saldo líquido do período.
- Mantém uma área de investimentos com aportes, resgates, saldo acumulado, histórico e gráfico em seis meses ou em todo o período. O saldo é calculado pelos movimentos informados; o app não consulta preços de mercado nem estima rentabilidade.
- Permite criar perfis locais independentes e alternar entre eles; lançamentos, orçamentos, categorias, metas e investimentos ficam separados por perfil.
- Compara os gastos com o período equivalente do mês anterior e exibe observações na área de Insights.
- Importa e exporta lançamentos em CSV.
- Salva automaticamente os dados do perfil ativo somente neste computador.
- Organiza o conteúdo em Visão geral, Movimentações, Orçamentos, Investimentos, Categorias, Insights e Dados e privacidade.
- Usa ícones vetoriais desenhados em Java2D para essas áreas, sem depender de glifos especiais instalados no Windows.
- Permite abrir a pasta local dos dados e importar/exportar CSV na área de privacidade.
- Filtra lançamentos por receita ou despesa e mantém o atalho de novo lançamento sempre visível.
- Sugere categorias personalizadas conforme o tipo do lançamento e atualiza planejado, gasto e restante enquanto você edita os orçamentos.
- Formata valores automaticamente ao sair do campo: `10000` vira `10000,00` tanto em lançamentos quanto em orçamentos.
- Aceita apenas números nos campos de data e valor, bloqueia sinais negativos e impede resgates maiores que o saldo investido disponível.
- Usa o ícone de carteira, moeda e seta aprovado na janela e na marca do app, com os arquivos `favicon.png` e `src/main/resources/orcabem-icon.png`.

## Requisitos

- Java 17 ou superior, com `javac` disponível no `PATH`.
- Windows: execute `run.bat`.

O aplicativo usa somente bibliotecas incluídas no JDK, sem Maven ou dependências externas.

## Executar

No Windows, dê dois cliques em `run.bat`, ou rode no terminal a partir desta pasta:

```bat
run.bat
```

O script compila o código para `build/classes` e inicia o OrçaBem.

## Ícone

O mesmo ícone é usado na barra de título, na barra de tarefas e na marca do app. A imagem de 512 × 512 está em `favicon.png` e em `src/main/resources/orcabem-icon.png`; `AppIcon` gera os tamanhos necessários para a interface.

## Armazenamento local

Os dados ficam em `%USERPROFILE%\.bolso-claro`. O perfil **Pessoal** usa diretamente os arquivos legados para preservar os dados existentes; cada perfil adicional recebe uma subpasta exclusiva em `profiles`. O catálogo guarda o nome e o perfil ativo:

```text
%USERPROFILE%\.bolso-claro\transacoes.csv
%USERPROFILE%\.bolso-claro\orcamentos.properties
%USERPROFILE%\.bolso-claro\categorias.properties
%USERPROFILE%\.bolso-claro\metas.properties
%USERPROFILE%\.bolso-claro\investimentos.properties
%USERPROFILE%\.bolso-claro\profiles.properties
%USERPROFILE%\.bolso-claro\profiles\<id-do-perfil>\transacoes.csv
%USERPROFILE%\.bolso-claro\profiles\<id-do-perfil>\orcamentos.properties
%USERPROFILE%\.bolso-claro\profiles\<id-do-perfil>\categorias.properties
%USERPROFILE%\.bolso-claro\profiles\<id-do-perfil>\metas.properties
%USERPROFILE%\.bolso-claro\profiles\<id-do-perfil>\investimentos.properties
```

O nome legado `.bolso-claro` é mantido intencionalmente para que usuários que atualizem a partir da versão anterior continuem vendo os mesmos dados. Para fazer backup, copie toda essa pasta. Remover os arquivos apaga os dados locais dos perfis. Para testar em outro local sem tocar nos dados normais, passe `-Dorcabem.data.dir=<caminho>` ao iniciar o Java.

## CSV

A importação aceita cabeçalho opcional e cinco colunas, nesta ordem: `data`, `tipo`, `categoria`, `descricao`, `valor`. A descrição pode ficar vazia. Datas em `AAAA-MM-DD` ou `DD/MM/AAAA`; tipo `receita` ou `despesa`. São aceitos arquivos separados por vírgula ou ponto e vírgula, com valores decimais usando ponto ou vírgula. Campos que contenham o separador devem estar entre aspas. Exemplo no formato brasileiro:

```csv
data;tipo;categoria;descricao;valor
2026-10-03;receita;Salário;Pagamento mensal;5200,00
2026-10-05;despesa;Restaurantes;"Almoço, sábado";74,50
```

A importação acrescenta os lançamentos aos dados existentes. A exportação inclui todos os lançamentos.

## Estrutura

```text
src/main/java/br/com/bolsoclaro/
  OrcaBemApp.java     interface Swing do OrçaBem e fluxo principal
  AppIcon.java        ícone vetorial do aplicativo, renderizado em vários tamanhos
  Transaction.java    modelo de receita/despesa
  CsvStore.java       armazenamento e leitura/escrita CSV
  BudgetStore.java    persistência de limites por mês e categoria
  CategoryStore.java  categorias pessoais de receitas e despesas
  SavingsGoalStore.java metas de economia mensais
  ProfileStore.java   catálogo de perfis locais independentes
  InvestmentStore.java histórico local de aportes e resgates
  TrendChart.java     gráfico Java2D de fluxo de caixa mensal
src/main/resources/
  orcabem-icon.png    ícone PNG do aplicativo
```
