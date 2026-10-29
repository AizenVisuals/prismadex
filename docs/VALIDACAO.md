# Validação da prévia 0.4.0

Verificada em Redmi Note 10S, Android 13, em 29/10/2026.

- Build Android otimizado e lint concluídos.
- 18 testes instrumentados passaram: catálogo/arte/learnsets, contas locais e visitante, persistência/migração, motor de dano, Meta, sets de equipes distintas em Singles/Doubles e isolamento de rascunhos.
- Teste manual: editar natureza numa equipe temporária, encerrar o processo, reabrir e recuperar o rascunho. Salvar e sair persistiu o set; a equipe temporária foi removida após o teste.
- Inspeção de Aurora claro/escuro e fonte ampliada a 1,3; configuração original restaurada.
- Amostra curta de oito gestos em grade, após ajuste de amostragem do vidro: 386 quadros, um com atraso (0,26%), mediana 9 ms, p90 11 ms. É uma amostra em um aparelho, não uma garantia de desempenho prolongado ou comparação controlada com versões anteriores.

A suíte instrumentada não é executada automaticamente pelo job de build sem emulador. Um workflow verde comprova apenas as etapas que ele executa.

Pendências: TalkBack completo, todos os temas/estados, tablets e dobráveis, múltiplos aparelhos, consumo sustentado e validação integral de regras competitivas. Nuvem e sincronização ainda não foram implementadas.
