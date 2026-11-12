# Android Java Lab

[![Android CI](https://github.com/lgmmoveis2020-del/android-java-lab/actions/workflows/android.yml/badge.svg)](https://github.com/lgmmoveis2020-del/android-java-lab/actions/workflows/android.yml)
![Java 17](https://img.shields.io/badge/Java-17-b07219)
![Material 3](https://img.shields.io/badge/Material-3-6750A4)
![minSdk 26](https://img.shields.io/badge/minSdk-26-3DDC84)
[![MIT](https://img.shields.io/badge/licen%C3%A7a-MIT-blue)](LICENSE)

Um app Android escrito **inteiramente em Java** que reúne oito laboratórios interativos.
Cada laboratório tem um motor em Java puro (testável na JVM, sem emulador) e uma tela
Material 3 por cima. Serve como playground, portfólio e referência de arquitetura
"núcleo testável + casca Android fina".

## Os laboratórios

| # | Laboratório | O que acontece | Núcleo (`core/`) |
|---|-------------|----------------|------------------|
| 1 | **Partículas** | Física 2D em tempo real: gravidade pelo acelerômetro, colisões elásticas, paredes saltitantes, explosões ao toque | `physics.ParticleSystem` — integração semi-implícita, grade espacial para colisões |
| 2 | **Jogo da Vida** | Autômato de Conway com borda toroidal, 7 padrões clássicos (glider, pulsar, canhão de Gosper…), desenho com o dedo | `life.LifeGrid`, `life.Pattern` |
| 3 | **Calculadora** | Parser de expressões com precedência, funções (`sin`, `sqrt`, `ln`…), constantes, variável `ans`, histórico e erro posicionado | `calc.Tokenizer` → `calc.Parser` → árvore `calc.Expr` |
| 4 | **Primos** | Crivo de Eratóstenes **segmentado** em thread de fundo com progresso e cancelamento; teste de Miller–Rabin determinístico até 2⁶³−1 | `primes.PrimeSieve` |
| 5 | **Labirinto** | Geração por backtracking recursivo e solução por BFS revelada célula a célula | `maze.MazeGenerator`, `maze.MazeSolver` |
| 6 | **Ordenação** | Seis algoritmos gravados passo a passo (comparações e escritas) e reproduzidos como barras animadas | `sort.SortRecorder`, `sort.SortAlgorithm` |
| 7 | **Notas** | CRUD com repositório, persistência em JSON (codec escrito à mão), swipe para excluir com *desfazer* | `notes.*`, `json.MiniJson` |
| 8 | **Sensores** | Leitura ao vivo de todos os sensores do aparelho + nível de bolha | `color.ColorMath` (interpolação) |

## Arquitetura

```
android-java-lab/
├── core/   Java puro (--release 17, -Werror). 28 classes, 219 testes JUnit 5.
│           Sem nenhuma dependência de Android: roda em qualquer JDK.
└── app/    Android (AGP 9, compileSdk 37, minSdk 26). Material 3, ViewBinding,
            ViewModel + LiveData, edge-to-edge, tema dinâmico, pt-BR + en.
```

Princípios:

- **Lógica fora do Android.** Tudo que dá para testar sem emulador vive em `core/`.
  O módulo `app/` só desenha, escuta toques e sensores e delega.
- **Uma Activity por laboratório**, todas herdando de `common.LabActivity`
  (edge-to-edge, toolbar com voltar, snackbar).
- **Trabalho pesado fora da UI thread** (`PrimeViewModel` com executor + cancelamento,
  `Choreographer` para a animação de partículas, `Handler` para os ticks do Jogo da Vida).
- **Catálogo declarativo**: `catalog/LabCatalog` lista os labs; a tela inicial é só um
  `RecyclerView` em grade (2 colunas no celular, 3 em tablets).

## Rodando

### Só o núcleo (não precisa de Android SDK)

```bash
./gradlew -PcoreOnly :core:test
```

### App completo

Abra no Android Studio (versão compatível com AGP 9.4) **ou**:

```bash
./gradlew :app:assembleDebug          # APK em app/build/outputs/apk/debug/
./gradlew :app:lintDebug              # lint falha em erros (traduções, APIs…)
./gradlew :app:connectedDebugAndroidTest   # Espresso, com emulador/aparelho conectado
```

Requisitos: JDK 17+, Android SDK com a plataforma 37 (o AGP baixa o que faltar).

### Verificações estáticas sem SDK

```bash
python3 scripts/check_resources.py
```

Confere que cada recurso referenciado existe, que `values/` e `values-en/` têm as
mesmas chaves de string (com os mesmos argumentos de formato) e que todo
`binding.xxx` usado em Java tem um `@+id/xxx` no layout correspondente.

## CI

O workflow [`android.yml`](.github/workflows/android.yml) roda em três jobs:
`core` (build + testes JVM), `resources` (checagem estática) e `app`
(testes unitários, lint e APK de debug publicado como artefato).

## Estrutura do código

```
core/src/main/java/com/lgm/androidlab/core/
  calc/      Tokenizer, Parser, Expr, Environment, Calculator
  color/     ColorMath (HSV, lerp, alpha, luminância)
  json/      MiniJson (parser/serializer JSON mínimo)
  life/      LifeGrid, Pattern
  maze/      Maze, Cell, Direction, MazeGenerator, MazeSolver
  notes/     Note, NoteRepository, InMemoryNoteRepository, NoteCodec
  physics/   Vec2, Particle, ParticleSystem
  primes/    PrimeSieve (segmentado + Miller–Rabin)
  sort/      SortAlgorithm, SortStep, SortRecorder

app/src/main/java/com/lgm/androidlab/
  MainActivity, LabApplication
  catalog/   LabItem, LabCatalog, LabAdapter
  common/    LabActivity, InsetsUtil, Labels
  labs/      particles/ life/ calculator/ primes/ maze/ sort/ notes/ sensors/
```

## Licença

[MIT](LICENSE).
