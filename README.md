# Bolso Claro

Aplicativo desktop de orçamento pessoal, feito em Java 17 e Swing. A navegação lateral, os cartões e o painel de fluxo de caixa seguem uma organização financeira clara, com identidade própria em verde-floresta, marfim e âmbar. Os dados ficam no próprio computador; não há conta, servidor ou banco em nuvem.

## O que faz

- Registra receitas e despesas com data, categoria, descrição opcional e valor.
- Permite editar ou excluir um lançamento selecionado e pesquisar por descrição ou categoria.
- Apresenta quanto sobra depois dos gastos, receitas, despesas e orçamento ainda disponível no mês selecionado.
- Mostra a distribuição percentual dos gastos por categoria e o fluxo de caixa de receitas e despesas nos últimos seis meses.
- Permite definir limites mensais por categoria e comparar o limite com o gasto realizado, com alertas em linguagem simples quando está perto ou foi ultrapassado.
- Permite adicionar e remover categorias de receitas e despesas. Lançamentos antigos e orçamentos salvos são preservados quando uma categoria sai da lista.
- Inclui uma meta de economia por mês com uma barra de progresso baseada no saldo líquido do período.
- Compara os gastos com o período equivalente do mês anterior e exibe observações na área de Insights.
- Importa e exporta lançamentos em CSV.
- Salva automaticamente lançamentos, orçamentos, categorias e metas no perfil local do usuário.
- Organiza o conteúdo em Visão geral, Movimentações, Orçamentos, Categorias, Insights e Dados e privacidade.
- Permite abrir a pasta local dos dados e importar/exportar CSV na área de privacidade.
- Filtra lançamentos por receita ou despesa e mantém o atalho de novo lançamento sempre visível.
- Sugere categorias personalizadas conforme o tipo do lançamento e atualiza planejado, gasto e restante enquanto você edita os orçamentos.
- Formata valores automaticamente ao sair do campo: `10000` vira `10000,00` tanto em lançamentos quanto em orçamentos.
- Usa um ícone vetorial próprio em verde-floresta e âmbar na janela e inclui o arquivo `favicon.png`.

## Requisitos

- Java 17 ou superior, com `javac` disponível no `PATH`.
- Windows: execute `run.bat`.

O aplicativo usa somente bibliotecas incluídas no JDK, sem Maven ou dependências externas.

## Executar

No Windows, dê dois cliques em `run.bat`, ou rode no terminal a partir desta pasta:

```bat
run.bat
```

O script compila o código para `build/classes` e inicia a interface gráfica.

## Ícone

O ícone da janela é desenhado em Java para aparecer na barra de título e na barra de tarefas. O PNG de 256 × 256 está em `favicon.png` e também em `src/main/resources/bolso-claro-icon.png`.

## Armazenamento local

Os arquivos são criados automaticamente em:

```text
%USERPROFILE%\.bolso-claro\transacoes.csv
%USERPROFILE%\.bolso-claro\orcamentos.properties
%USERPROFILE%\.bolso-claro\categorias.properties
%USERPROFILE%\.bolso-claro\metas.properties
```

Para fazer backup, copie essa pasta. Remover os arquivos apaga os dados locais do app.

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
  BolsoClaroApp.java   interface Swing e fluxo principal
  AppIcon.java        ícone vetorial do aplicativo, renderizado em vários tamanhos
  Transaction.java    modelo de receita/despesa
  CsvStore.java       armazenamento e leitura/escrita CSV
  BudgetStore.java    persistência de limites por mês e categoria
  CategoryStore.java  categorias pessoais de receitas e despesas
  SavingsGoalStore.java metas de economia mensais
  TrendChart.java     gráfico Java2D de fluxo de caixa mensal
src/main/resources/
  bolso-claro-icon.png ícone PNG do aplicativo
```
