# Fontes e limites dos dados

Consulta de Champions revisada em 28/10/2026. Não há atualização automática do catálogo nesta prévia.

| Recorte | Espécies | Registros com formas |
|---|---:|---:|
| Nacional | 1.025 | 1.306 |
| Seleção Champions revisada | 231 | 354 |
| Entradas diretamente relacionadas à lista oficial M-C | 231 | 262 |
| Formas/variantes suplementares da referência de batalha | — | 92 |

Presença no catálogo, presença no jogo e elegibilidade em um regulamento são conceitos diferentes. A lista oficial de elegíveis não enumera todas as transformações de batalha nem comprova a Pokédex integral do jogo. Uma ausência nessa página não basta para concluir que o Pokémon inexiste em Champions.

- [Regulamento oficial M-C](https://champions-news.pokemon-home.com/en/page/816.html): de 09/09/2026 02:00 UTC a 02/12/2026 01:59 UTC.
- [Lista oficial de elegíveis](https://web-view.app.pokemonchampions.jp/battle/pages/events/rs178713870219xeaaio/en/pokemon.html).
- [Pokémon Showdown — dados Champions](https://github.com/smogon/pokemon-showdown/tree/1242c1e2d9763ba5b85d33f13aecb89bae19ae3d/data/mods/champions), commit fixado de 27/10/2026. Usado no cruzamento de atributos, habilidades, golpes e itens. Licença MIT preservada.
- [Smogon Damage Calculator](https://github.com/smogon/damage-calc/tree/c30417b84b558b407ea50ebba4b4bcc603f6fbff): motor Champions empacotado e executado offline em Rhino. O código TypeScript de referência está em `vendor/smogon-calc/`.
- [PokeAPI](https://github.com/PokeAPI/pokeapi) e [sprites](https://github.com/PokeAPI/sprites): identidade e artes. A pesquisa de referência local também foi usada na montagem do catálogo e das tabelas históricas; dados gerais e históricos não receberam certificação integral por versão. Artes e personagens mantêm os direitos de seus titulares.
- [Smogon Stats](https://www.smogon.com/stats/): Meta mensal do simulador. Rankings oficiais e snapshots externos não são misturados a essas porcentagens.

Na revisão 0.3.0, Rillaboom foi incluído, Indeedee macho/fêmea e Meowstic Mega por sexo foram separados, 13 IDs duplicados migrados e dez imagens de formas incorretas corrigidas. A 0.4.0 manteve esse catálogo e melhorou os fluxos.

Existem 166 itens e 515 definições de golpes específicos de Champions. Formas cosméticas ou de batalha podem compartilhar aprendizados quando isso é explícito na fonte. Não deduplicar espécies apenas porque compartilham arte.

Ainda faltam validação de todas as combinações competitivas, cobertura histórica completa e atualização por novos regulamentos. O catálogo nacional é consulta adicional, não uma promessa de que toda espécie possa ser usada em Champions.

Na 0.5.0, cada um dos 166 itens recebe seu próprio sprite offline, mapeado pelo nome canônico e mantendo o ID salvo. Os 515 golpes de Champions passam a expor descrição, resumo, prioridade, alvo e propriedades. Os 17 textos específicos de Champions têm prioridade sobre a descrição geral do mesmo golpe. Fontes e versões estão nos [avisos técnicos](../android/THIRD_PARTY_NOTICES.md). Descrições originais permanecem em inglês, com indicação visível; a tradução da interface não é apresentada como tradução oficial do jogo.
