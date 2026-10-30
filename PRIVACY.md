# Privacidade — PrismaDex

Última revisão: 29 de outubro de 2026. Aplica-se à prévia Android 0.6.0-preview da Aizen Visuals.

## O que fica no aparelho

Equipes, sets, alvos de teste, rascunhos, favoritos, tema e preferências são armazenados localmente. Contas por e-mail/senha também são locais; esta versão não possui servidor de contas, confirmação de e-mail, recuperação remota ou sincronização entre aparelhos.

Senhas usam salt aleatório e PBKDF2-HMAC-SHA256. Os demais dados locais não recebem criptografia própria do app. A proteção disponível depende também do Android e do bloqueio do aparelho. Uma conta local não equivale a um backup online.

## Internet e fontes externas

A aba Meta consulta relatórios públicos do Smogon/Pokémon Showdown. Como em qualquer acesso à internet, o servidor de destino pode receber informações de conexão, como endereço IP. O app não envia seu e-mail, senha, equipes ou favoritos nessa consulta. Links para sites externos são abertos no navegador e seguem as políticas desses sites.

Catálogo, imagens, golpes, equipes e cálculo de dano não dependem de conexão. A última consulta de Meta pode permanecer em cache.

## Anúncios e métricas

A implementação 0.6.0-preview não integra publicidade nem SDK de analytics. O download pelo GitHub e os sites externos têm suas próprias políticas.

## Excluir dados

Desinstalar o app ou usar “Limpar dados” nas configurações do Android pode apagar as informações locais. Ainda não existe recuperação de equipes pela nuvem. Evite fazer isso para atualizar um APK com assinatura compatível.

## Contato

Aizen Visuals: **aizenvisuals.apps@gmail.com**. Para problemas públicos, use as Issues do repositório. Não envie senhas, dados de conta ou outros dados pessoais em uma Issue.
