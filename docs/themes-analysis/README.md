# Temas (6) + ícone — análise e proposta (APROVAÇÃO PENDENTE)

Branch: `wip/continuacao-2026-10-30`, a partir de `af7b869`.
NADA desta proposta foi implementado no app: nenhum token em
`SettingsScreen.kt` foi alterado e o launcher em `res/` está intacto.
Amostras em `theme-swatches.svg/.png` são RENDERIZADAS a partir dos
tokens — não são screenshots do aparelho. A única captura de aparelho
desta fase é a home do build antigo (ver relatório de continuação).

## 1. Como o sistema de temas funciona (auditoria)

`SettingsScreen.kt` — `AppThemes`: 6 temas, cada um com SÓ 3 tokens
por variante: `accent`, `background`, `surface`. Todo o resto é fixo
em `PrismadexApp.kt`: texto escuro `#28263A` / variante `#625A75`
(claro) e `#F2F0FA` / `#B5B1C5` (escuro), `surfaceVariant` = surface
com alpha 0,85. Ou seja: trocar de tema muda fundo, superfície e
acento — e mais nada. As cores de tipo dos cards, o vidro da barra e
o texto secundário não mudam nunca.

Tokens atuais (hex, sem alpha):

| Tema | dark accent | dark bg | dark surface | day accent | day bg | day surface |
|---|---|---|---|---|---|---|
| Aurora (principal) | C4B5FD | 10121B | 1A1C29 | 6750A4 | F4F0FF | FFFFFB |
| Carmim | FF9EB5 | 1B1017 | 2B1C28 | A32D50 | FFF1F4 | FFFAFB |
| Oceano | 8CDAF4 | 0B1720 | 172B38 | 006785 | EDF8FD | FAFDFF |
| Jade | 9BE2BE | 0C1915 | 1A2D25 | 1D6B48 | EFF8F1 | FAFFFB |
| Âmbar | F4D18C | 1B160E | 2D261A | 805409 | FFF7E8 | FFFCF7 |
| Pérola | CBCBDF | 15151B | 22222B | 5C5D75 | F3F3F5 | FFFFFF |

## 2. Contraste WCAG (calculado, não estimado)

Accent/fundo e accent/superfície, todos ≥ 4,5:1 em TODOS os temas
(escuro 9,2–12,3; claro 5,4–6,7). Texto fixo sobre fundo/superfície
também passa em todos (≥ 5,8). **Contraste NÃO é o problema.**

Separação superfície/fundo (o quanto um card "levanta" do fundo):
Aurora 1,10/1,12 · Carmim 1,15/1,06 · Oceano 1,24/1,06 ·
Jade 1,24/1,07 · Âmbar 1,20/1,04 · Pérola 1,15/1,11
(escuro/claro). Aurora é a PIOR no escuro — separação também não
explica por que ela é a boa.

## 3. Por que os outros parecem fracos, então

1. **Coerência com a marca, não números.** O acento da Aurora É a
   marca: o gradiente do ícone é lilás (`#E3D9FF → #AE96ED`), o
   gosto declarado do usuário é grafite + lilás, e o texto variante
   fixo (`#B5B1C5` / `#625A75`) é lilás-acinzentado. Nos outros
   temas, o acento muda mas o ícone do launcher continua lilás, as
   pastéis de tipo continuam as mesmas e o texto secundário continua
   lilás — o tema lê como "Aurora com uma tinta", não como um tema
   próprio.
2. **Acento monocromático com o fundo.** Em todos os escuros, o matiz
   do acento está a 0–24° do matiz do fundo (Âmbar: 3°; Pérola: 0°).
   Não há tensão complementar; é uma lavagem de uma cor só.
3. **Claro quase branco.** Fundos claros têm saturação ≤ 0,09 e as
   superfícies são praticamente brancas — a identidade do tema no
   claro vive só no acento e numa tinta faint do fundo.
4. **Pérola é a exceção real.** Acento escuro `#CBCBDF`: saturação
   0,09, distância RGB ao texto `#F2F0FA` de apenas 60 (os outros:
   75–114) — o acento mal se distingue do texto. No claro,
   `#5C5D75` (S 0,21) lê como texto cinza/desabilitado, não como
   acento. Pérola não tem identidade cromática própria.
5. **Limite estrutural (fora do escopo mínimo).** Sem tokens
   secundários/por-tema, nenhum tema pode ficar tão coerente quanto
   a Aurora — isso exigiria novos primitivos (texto variante por
   tema, superfícies de container, acento-sobre-acento) e é uma
   decisão maior, registrada aqui e NÃO proposta como "mínima".

## 4. Correções mínimas propostas (APROVAÇÃO NECESSÁRIA)

Regra: manter a família de matiz de cada tema, mexer em 1–2 tokens
por tema, nunca tocar na Aurora (referência, congelada). Contrastes
recalculados após cada valor proposto — todos ≥ 4,5:1.

- **Pérola escuro — acento `#CBCBDF → #9D97CC`.** Saturação
  0,09 → 0,26; contraste/fundo 11,38 → 6,69 (ainda passa);
  distância ao texto 60 → 131. Trade-off honesto: a distância ao
  texto variante fixo lilás (`#B5B1C5`) cai de 43 para 36 — dentro
  do sistema de 3 tokens não dá para afastar dos dois textos ao
  mesmo tempo; o primário venceu.
- **Pérola claro — acento `#5C5D75 → #4E4F78`.** Saturação
  0,21 → 0,35; contraste/fundo 5,78 → 6,98.
- **Fundos claros — aprofundar a tinta em um passo** (superfície
  já é branca; a separação só pode vir do fundo):
  Carmim `#FFF1F4 → #FFE3EA` (sep. 1,06 → 1,17; acento/fundo
  6,28 → 5,72) · Oceano `#EDF8FD → #DDF2FB` (1,06 → 1,13;
  5,93 → 5,54) · Jade `#EFF8F1 → #DDF0E2` (1,07 → 1,18;
  5,96 → 5,43) · Âmbar `#FFF7E8 → #FFEFD2` (1,04 → 1,11;
  6,18 → 5,81).
- **Carmim / Oceano / Jade / Âmbar escuros — NENHUMA mudança
  proposta.** Saturação do acento (0,31–0,43) e contraste já estão
  no padrão; mexer seria gosto, não correção.

Aplicar = editar só os 6 valores acima em `AppThemes` + capturas
antes/depois no aparelho. Sem aprovação exata, nada muda.

## 5. Ícone — auditoria do atual

- Launcher: SÓ adaptive icon (`mipmap-anydpi-v26/ic_launcher.xml`,
  fundo `#141322` + foreground `ic_prisma_foreground.xml`). Sem
  densidades legacy — aceitável porque `minSdk = 26`.
- A marca é um "ball-mark": anel + banda horizontal + botão central,
  girado −25°, traço 3,48 em viewport 108, com um grupo de escala
  0,525 — ou seja, o anel ocupa ~28% da largura e o traço efetivo
  é ~1,8dp. No launcher ele some no grafite; a banda some junto.
  Renderização fiel em `../icon-candidate/icon-current-512.png`.
- Dentro do app, `PrismaMark` (`ic_prisma_mark.xml`) é tingido com o
  acento do tema (gradiente perdido, traço fino some a 28–36dp) —
  é por isso que "dentro do app fica feio". `MetaScreen` usa o
  foreground sem tinta como placeholder de arte ausente.
- `ic_prisma.xml` (versão quadrada cheia, sem rotação) não tem uso
  encontrado em código/XML — morto aparente, não tocado.

## 6. Ícone candidato (APROVAÇÃO PENDENTE)

`../icon-candidate/ic_prisma_foreground_candidate.xml` (vetor) +
previews 512/192/96/48. NÃO está em `res/` — não pode vazar para o
build por acidente.

Mesma marca (anel + banda + botão, −25°, tokens Aurora SÓ: grafite
`#141322`, gradiente lilás `#E3D9FF → #AE96ED`, branco em alpha
para o brilho), com três mudanças medidas: anel maior (r 30 → 34 na
viewport 108), traço 3,48 → 5 (com escala removida), brilho de
vidro (arco branco 0,5 alpha no quadrante superior-esquerdo) +
halo lilás 0,12 alpha atrás da marca (identidade Liquid Glass sem
nenhum primitivo novo). Verificado visualmente em 512px (banda
legível) e 48px (anel + botão legíveis; a banda some em sub-pixel,
como em qualquer marca fina — aceito e registrado).

Se aprovado: substituir SÓ o foreground do launcher; decidir em
separado se `PrismaMark` passa a usar o candidato sem tinta (o
tingimento atual destruiria o gradiente) — fora desta proposta.

## 7. Impeccable — o que foi e não foi usado, e por quê

- `scripts/impeccable context` rodado 1× (PRODUCT.md 29/10/2026,
  sem CONTEXT_STALE). SKILL.md v4.8.0 + `reference/craft.md`,
  `bolder.md`, `craft-floor.md` lidos.
- `craft`: NÃO usado como passo separado — é alias depreciado de
  um pedido comum ("adds nothing", diz o próprio doc); o trabalho
  seguiu o roteamento normal, como na fase de polish.
- `bolder`: NÃO aplicado. Bolder proíbe adicionar cores/primitivos
  sem aprovação e amplifica superfícies existentes — esta fase é
  ANÁLISE + PROPOSTA de adições que exigem aprovação; aplicar
  bolder aqui seria implementar por baixo do aval. As regras de
  contraste do `craft-floor.md` (texto ≥ 4,5:1) foram usadas como
  critério de verificação dos valores propostos.
- `live`/`generate`: web-only, recusados — nenhum frontend web.
