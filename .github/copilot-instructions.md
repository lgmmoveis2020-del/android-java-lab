# Instruções para o Copilot — android-java-lab

App Android **100 % Java** (sem Kotlin) dividido em dois módulos Gradle:

| Módulo | O que é | Como validar |
|--------|---------|--------------|
| `core/` | Motores puros (física, Game of Life, parser da calculadora, crivo de primos, labirinto, ordenação, notas/JSON, cores). Zero dependência de Android. | `./gradlew -PcoreOnly :core:build` (JUnit 5, roda em qualquer JDK 17+) |
| `app/`  | UI Material 3 com uma Activity por laboratório, ViewBinding, ViewModel/LiveData. | Só compila com o Android SDK (CI). Antes de abrir PR rode `python3 scripts/check_resources.py`. |

## Regras que quebram o build se ignoradas

1. **`core` não pode importar `android.*` nem `androidx.*`.** Ele compila com `-Xlint:all -Werror` e `--release 17`; qualquer warning vira erro.
2. **Strings:** toda chave de `app/src/main/res/values/strings.xml` precisa existir em `values-en/strings.xml` (e vice-versa). Exceção: `translatable="false"` só em `values/`. O lint trata `MissingTranslation` como erro e `ExtraTranslation` como fatal.
3. **R não transitivo** (`android.nonTransitiveRClass=true`): atributos de tema vêm do pacote que os declara — `androidx.appcompat.R.attr.colorPrimary`/`colorError` (AppCompat) e `com.google.android.material.R.attr.colorOnSurfaceVariant`, `colorSurface`… (Material). Nunca `R.attr.colorPrimary` do app.
4. **APIs Java:** `minSdk 26`. Use só o que o D8 faz backport (`List.of`, `String.isBlank/strip`, records, switch com seta). Evite `Stream.toList()`, `Collectors.toUnmodifiable*`, `Math.clamp`, pattern matching em `switch`.
5. **Material 3:** estilos `Widget.Material3.*`; tema `Theme.AndroidLab` (pai `Theme.Material3.DayNight.NoActionBar`). Telas estendem `common.LabActivity` e chamam `setupScreen(root, toolbar)` para edge-to-edge + seta de voltar.

## Convenções

- Pacote raiz `com.lgm.androidlab`; cada laboratório em `labs/<nome>/` com `XxxLabActivity` (+ `XxxView` custom para desenho, `XxxViewModel` para trabalho em segundo plano).
- Novo laboratório = engine em `core` **com testes** → Activity/layout em `app` → entrada em `catalog/LabCatalog` → strings pt-BR **e** en → `activity` no `AndroidManifest.xml` com `parentActivityName=".MainActivity"`.
- Comentários e strings padrão em **português do Brasil**; identificadores em inglês.
- Sem dependência nova sem necessidade; versões ficam em `gradle/libs.versions.toml`.
- Testes do `core` em JUnit 5 (`core/src/test`), do `app` em JUnit 4 (`app/src/test`) e Espresso (`app/src/androidTest`).

## Comandos úteis

```bash
./gradlew -PcoreOnly :core:test              # testes do núcleo (sem SDK)
python3 scripts/check_resources.py           # strings, ids, referências de recursos
./gradlew :app:lintDebug :app:assembleDebug  # precisa do Android SDK
```
