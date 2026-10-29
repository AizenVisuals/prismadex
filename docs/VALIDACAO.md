# Validação das prévias

## 0.4.1-preview

- Build otimizado e lint concluídos, sem erros. Atualização instalada por cima da anterior, com o mesmo certificado.
- Grade inspecionada no Redmi Note 10S: Charizard/Mega Charizard e Mega Charizard Y/Blastoise alinhados. Diferença máxima de um pixel entre as colunas, decorrente do arredondamento de largura.
- Grade conferida com fonte normal (0,92) e ampliada (1,3); configuração original restaurada. Duas posições de tipagem e duas linhas de nome reservadas em cada card.
- Tipagens compactas e rótulos da navegação receberam altura de linha explícita; rótulos da navegação têm margem lateral e truncamento.
- Alteração de apresentação; os 18 testes de domínio abaixo pertencem à versão 0.4.0 e não foram repetidos nesta correção.

## 0.4.0-preview

Verificada em Redmi Note 10S, Android 13, em 29/10/2026.

- Build Android otimizado e lint concluídos.
- 18 testes instrumentados passaram: catálogo/arte/learnsets, contas locais e visitante, persistência/migração, motor de dano, Meta, sets de equipes distintas em Singles/Doubles e isolamento de rascunhos.
- Teste manual: editar natureza numa equipe temporária, encerrar o processo, reabrir e recuperar o rascunho. Salvar e sair persistiu o set; a equipe temporária foi removida após o teste.
- Inspeção de Aurora claro/escuro e fonte ampliada a 1,3; configuração original restaurada.
- Amostra curta de oito gestos em grade, após ajuste de amostragem do vidro: 386 quadros, um com atraso (0,26%), mediana 9 ms, p90 11 ms. É uma amostra em um aparelho, não uma garantia de desempenho prolongado ou comparação controlada com versões anteriores.

A suíte instrumentada não é executada automaticamente pelo job de build sem emulador. Um workflow verde comprova apenas as etapas que ele executa.

Pendências: TalkBack completo, todos os temas/estados, tablets e dobráveis, múltiplos aparelhos, consumo sustentado e validação integral de regras competitivas. Nuvem e sincronização ainda não foram implementadas.
