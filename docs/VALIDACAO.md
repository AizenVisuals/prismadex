# Validação das prévias

## 0.5.0 — build local, sem publicação

Verificada em Redmi Note 10S, Android 13, por depuração Wi-Fi em 29/10/2026.

- APK otimizado e lint concluídos. Certificado de desenvolvimento preservado; atualização instalada sem limpar dados. Hash do APK instalado comparado ao arquivo local e idêntico.
- Suíte de 23 testes instrumentados: 22 passaram na execução completa; um teste de tradução expôs uma frase manual sobrescrita por template. A prioridade foi corrigida e os 5 testes do conjunto de idioma/descrições passaram na repetição. As 23 condições distintas ficam cobertas pelas execuções. Teste unitário de remoção isolada também passou.
- Equipe temporária: inclusão de Venusaur, cancelamento da remoção preservando 1/6, confirmação produzindo 0/6 e exclusão somente da equipe de teste. Equipe original e favoritos preservados.
- Capturas nativas de Pokédex, Favoritos, Equipes/ações finais, calculadora, Meta, configurações, conta e fontes. Conferidos texto 100%/150%, tema claro/escuro e paisagem no telefone. Navegação lateral em paisagem; rótulo ativo amplo na navegação compacta com texto grande.
- Slider de texto testado por arrasto real: prévia local e aplicação global ao soltar, sem deslocar o alvo durante o gesto. Preferência 100% restaurada.
- Descrições/resumos têm cobertura PT/ES em 1.238 textos distintos; inglês original preservado. 182 textos revisados ou derivados de templates humanos incluem os 17 registros de efeitos específicos de Champions, além de efeitos comuns. Auditoria numérica sem diferenças; isso não certifica revisão linguística integral das demais traduções longas.
- Idioma espanhol conferido na ficha de Protect e em componentes nativos, incluindo rótulos da folha. Nome original, PP 5 e prioridade +4 preservados. Retorno a português confirmado.
- Amostra curta de seis gestos em grade: 355 quadros, 3 com atraso (0,85%), mediana 8 ms e p90 13 ms. A amostra anterior foi 313 quadros, 1,60%, mediana 10 ms e p90 17 ms, mas posição e aquecimento de cache não são equivalentes; não prova ganho percentual controlado.
- Preferências ao encerrar: português, texto 100%, paleta Pérola/modo Sistema, visualização em lista. Rotação, tempo de tela e opção de manter ligado preservados. Nenhum push/release autorizado ou executado.

Permanecem sem certificação: todos os 12 temas em todos os estados, tablets/dobráveis físicos, TalkBack integral, Androids além do aparelho testado, consumo sustentado e revisão humana integral dos textos longos.

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

## Correção local da abertura do Meta — 29/10/2026

- Associação de identidades retirada da composição e executada em `Dispatchers.Default`, com índice exato em vez de buscas repetidas pelo catálogo. A precedência entre nomes canônicos, aliases e nomes da interface foi preservada.
- Cache, inclusive vencido, exibido antes da atualização online; atualizar mantém o relatório visível. Leitura/gravação em IO e cancelamento propagado ao sair/trocar formato.
- Quatro testes instrumentados passaram no Redmi: equivalência de todas as 341 identidades do relatório de teste (incluindo distinção Indeedee-F), cache vencido como primeira emissão sem depender de rede, parser e consulta/cache real separados por Singles/Doubles. Log: `research/meta-lag-tests-final.log`.
- No APK debug, o teste do mesmo lote levou 9.467 ms pelo método anterior e 377 ms com índice (inclui construção); isso mede associação, não o tempo total de abertura nem a versão otimizada. Evidência: `research/meta-lag-identity-timing.txt`.
- Build debug/test/preview e lint passaram. Preview instalado por cima, hash do APK instalado conferido: `872bc36e3be30e93b0af3898824c4c512fdbdfefc906f88a33c20c81869995e0`.
- Abertura, Singles/Doubles e atualização inspecionados no aparelho. Capturas `research/preview/impeccable/meta-lag-*.png`. Amostra fria de 32 quadros ainda teve atrasos (7 pelo gfxinfo); não certifica ausência de jank nem desempenho sustentado.
- APK local: `releases/PrismaDex-0.5.0-local-meta-872bc36e.apk`. Nenhuma publicação no GitHub.

Navegação adicional: três retornos à aba Meta concluídos com relatório Doubles visível. Amostra curta: 183 quadros, 10 atrasados (5,46%), mediana 16 ms, p95 34 ms; ainda há atrasos ocasionais de renderização. Log `research/meta-lag-after-warm-navigation-gfx.txt`.

## Fichas com navegação lateral e evoluções — local, 29/10/2026

- Deslizar à esquerda abre o próximo Pokémon; à direita volta. A ordem aberta na Pokédex preserva filtros/busca/ordenação e formas. Não há volta circular nos extremos. Destaques fora da lista usam ordem nacional. Botões anterior/próximo oferecem alternativa ao gesto. Troca bloqueada enquanto há rascunho de set pendente; set editor não captura esse gesto.
- Linha evolutiva offline na aba Informações: artes, ramificações, métodos alternativos PT/EN/ES e nomes originais de golpes/itens; tocar numa etapa abre a ficha exata. Mega é identificada como transformação; ausência de dados para uma forma não é confundida com ausência de evolução.
- Análise REA da referência e extração: 575 famílias, 1.095 identidades, 525 ligações e cobertura das 1.025 espécies nacionais. Base anterior do APK, suplementada com PokeAPI versionado para Leaf/Ice Stone e Rockruff/Own Tempo. Arquivo e evidências: `research/evolution/REA-EVOLUTION.md`. Dados não certificam métodos atuais para todos os jogos nem uma mecânica de evolução dentro de Champions; interface informa os limites.
- Build debug/test/preview, teste unitário de navegação e lint passaram (`research/evolution-browse-final-build.log`). Cinco testes instrumentados de evolução passaram no Redmi (`research/evolution-tests-final.log`), incluindo ramificações, formas regionais, Mega/singleton/desconhecido, métodos localizados, casos recentes suplementados e Snom.
- Conferência física: Raichu → Alolan Raichu → Raichu por gestos, cabeçalho conferido em cada etapa; Rillaboom mostrou Grookey → Thwackey (nível16) → Rillaboom (nível35), e tocar em Thwackey abriu essa identidade. Capturas/árvores em `research/preview/impeccable/pokemon-swipe-both-directions.*`, `evolution-rillaboom-final.*`, `evolution-open-relative-final.*`. A amostra é funcional/visual; não certifica desempenho sustentado ou TalkBack completo.
- APK otimizado instalado por cima e hash instalado igual ao arquivo: `c546dc5b9eb5bb0a0dfdebc7a43229dc799e001bdc3fbb2c9269df7dd72304e3`. Cópia `releases/PrismaDex-0.5.0-local-evolution-c546dc5b.apk` (20.236.652 bytes).
- Nenhum push, tag ou release no GitHub; autorização de publicação continua revogada. Contas, favoritos e equipes preservados.
