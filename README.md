# 🚀 Teste de Pressão — App Android de Telemetria e Diagnóstico OBD2

Aplicação Android desenvolvida em **Kotlin** e **Jetpack Compose** para monitorização em tempo real de turbocompressor, gravação de viagens, cálculo de consumos de combustível, avaliação inteligente do estilo de condução, teste de aceleração dos 0 aos 100 km/h, scanner de erros e ferramentas dedicadas ao **Peugeot 508 II (2020)**.

![App Logo](app_logo.jpg)

---

## 🏎️ Funcionalidades Principais (Versão 1.2.0)

### 1. Novo Trip Computer & Gravador de Viagens
* **Cálculo de Consumo em Tempo Real:**
  * **Em andamento:** Apresenta o consumo instantâneo e o consumo médio em **L/100 km**.
  * **Parado em semáforos/trânsito:** Comuta automaticamente para **L/h (Litros por hora)**, exatamente como o computador de bordo original da carrinha.
  * **Litros Gastos:** Medição precisa do total de combustível consumido durante a viagem.
* **Avaliação Inteligente da Condução:**
  * **🍃 Económica (Eco-Score 75-100):** Condução suave, acelerações progressivas e baixas rotações.
  * **⚡ Dinâmica (Eco-Score 50-74):** Ritmo ágil de dia a dia, acelerações equilibradas e boa fluidez de trânsito.
  * **🏎️ Desportiva (Eco-Score < 50):** Condução agressiva com fortes pressões de turbo, acelerações a fundo e rotações elevadas.
* **Histórico de Trajetos:** Guarda todas as viagens na memória com data, distância (km), duração, consumos, velocidade média/máxima e o selo de estilo de condução atribuído.

### 2. Manómetro de Pressão de Turbo em Tempo Real (Universal)
* Leitura direta do sensor MAP (`010B`) subtraindo a pressão atmosférica barométrica (`0133`).
* Manómetro desportivo de alta precisão (-1.00 a +3.00 Bar) com animação suave e memória de pico máximo (*Peak Hold*).
* Telemetria instantânea: RPM (`010C`), Velocidade (`010D`), Temperatura da água (`0105`) e pressão absoluta no coletor.

### 3. Dashboard Completo de Sensores com Filtros
Grelha dinâmica em tempo real para todos os sensores do Peugeot 508:
* **Temperatura do Óleo (`015C`):** Monitorize o aquecimento do lubrificante no motor antes de puxar pelo turbo.
* **Tensão da Bateria & Alternador (`0142`):** Verifique a saúde elétrica e carga do alternador (~14.2V).
* **Carga do Motor (`0104`):** Percentagem de esforço da centralina (0 a 100%).
* **Temperatura de Admissão (`010F`):** Temperatura do ar pós-intercooler.
* **Pedal de Acelerador (`0111`), Fluxo MAF (`0110`), Combustível (`012F`) e Tempo de Motor Ligado (`011F`).

### 4. Cronómetro de Aceleração 0 aos 100 km/h
* Deteção e início de contagem automáticos no momento do arranque.
* Trava milissegundo a milissegundo e bloqueia aos 100 km/h, registando o melhor tempo pessoal.

### 5. Diagnóstico e Leitor de Avarias (DTC)
* Leitura de códigos de erro da centralina (Modo `03`) traduzidos para português.
* Apagador de avarias e limpeza da luz de aviso do motor (*Check Engine* - Modo `04`).

### 6. Definições, Ecrã Sempre Ligado & Atualizador In-App
* **Manter Ecrã Sempre Ligado:** Não deixa o ecrã do telemóvel desligar enquanto conduz.
* **Ativar/Desativar Sensores:** Permite selecionar apenas os sensores pretendidos para acelerar a taxa de atualização do turbo.
* **Atualizador Integrado:** Verificação e instalação de atualizações do GitHub com 1 toque sem sair da app!

---

## 📦 Como Instalar e Atualizar

1. Se já tem a versão v1.1.0 instalada, vá ao separador **Definições** dentro da app e toque em **"Verificar Atualizações"**!
2. Ou descarregue o ficheiro **`TesteDePressao.apk`** diretamente do repositório no seu telemóvel Android.
