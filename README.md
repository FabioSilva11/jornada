# Jornada

App Android para marcar os dias de trabalho, ver quanto cada dia rende (por semana, quinzena ou mês), descontar despesas do salário e calcular as férias (regra CLT ou reserva para autônomo).

- Kotlin + Jetpack Compose (Material 3)
- Dados salvos só no celular (JSON), com backup e exportação CSV
- Lembretes com WorkManager

## Compilar

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

Fonte Plus Jakarta Sans sob a licença SIL OFL (`licenses/`).
