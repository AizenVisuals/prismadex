# PrismaDex Android

Implementação Kotlin + Jetpack Compose + Material 3 da Aizen Visuals. Android mínimo API 26; compile/target SDK 35, JDK 17, Gradle Wrapper incluído.

A apresentação pública, instalação e limites da prévia estão no [README principal](../README.md). [Fontes dos dados](../docs/DADOS.md), [validação](../docs/VALIDACAO.md), [licenças](THIRD_PARTY_NOTICES.md) e [armazenamento local](docs/DATA.md).

```sh
./gradlew :app:assemblePreview :app:lintDebug
./gradlew :app:connectedDebugAndroidTest
```

Configure o SDK por `ANDROID_HOME` ou `local.properties` (arquivo privado, ignorado pelo Git). O APK de prévia fica em `app/build/outputs/apk/preview/`.

Código em `app/src/main/java/dev/bloody/prismadex/`; dados, artes e motor offline em `app/src/main/assets/`. Alterar o identificador do aplicativo ou a assinatura pode impedir uma atualização compatível. Não publicar chaves de assinatura, bases de contas ou dados de aparelho.

Contas são locais, sem nuvem; o cálculo usa o set salvo, não o rascunho. APKs compilados em máquinas diferentes podem ter certificados de desenvolvimento distintos. O CI verifica o projeto, mas não distribui seus APKs automaticamente.
