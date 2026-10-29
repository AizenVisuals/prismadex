# Idioma e texto

A interface tem português brasileiro, inglês e espanhol. `DisplayPreferences` guarda idioma e escala de texto no aparelho, separados da conta/equipe. Sistema segue português/espanhol quando reconhecidos e usa inglês para os demais idiomas.

`ui-languages.json` é o catálogo local: cada linha contém texto de origem em português, inglês e espanhol. Slots dinâmicos usam `{0}`, `{1}` na ordem em que aparecem na origem. `UiStrings` compila os padrões uma vez por idioma e limita o cache de resultados a 1.024 entradas. Nenhuma tradução depende de rede. `LText` e `LIcon` aplicam o catálogo aos textos e descrições acessíveis; nomes, e-mails e valores digitados não são traduzidos. A busca nos seletores aceita o rótulo traduzido e o original.

Nomes de Pokémon, itens e golpes preservam a referência canônica. `localized-descriptions.json` contém descrições/resumos em português e espanhol: 1.238 textos de origem distintos, cobrindo os 934 golpes da base e descrições inglesas dos itens. Descrições especiais das Mega Stones usam templates traduzidos, preservando nomes das formas. O inglês usa a fonte original; a ficha permite consultar o original nos demais idiomas.

A geração inicial usou modelos locais de tradução Argos/OPUS via CTranslate2, somente como ferramenta de desenvolvimento. Nenhum modelo ou serviço de tradução é incluído/executado no Android. 182 textos têm revisão manual ou templates humanos, incluindo os 17 registros de efeitos específicos de Champions (alguns compartilham texto). Cobertura e preservação numérica foram verificadas integralmente; isso não equivale a revisão linguística integral das descrições longas. Os números, PP, prioridade, alvo e regras do motor nunca são calculados a partir da tradução.

A escala oferece 85%, 100%, 115%, 130% e 150%. É multiplicada pela fonte do Android, preservando a densidade física e os insets. O controle tem prévia local durante o arrasto, aplica a escala global ao soltar e oferece botão de restaurar 100%. A aplicação no fim do gesto evita deslocar o slider sob o dedo. Não alterar o tamanho de texto do sistema para simular essa preferência.
