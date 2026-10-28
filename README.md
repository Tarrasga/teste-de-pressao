# 🚀 Teste de Pressão — App Android de Telemetria e Diagnóstico OBD2

Aplicação Android desenvolvida em **Kotlin** e **Jetpack Compose** para monitorização em tempo real de turbocompressor, teste de aceleração dos 0 aos 100 km/h, leitura/limpeza de códigos de erro e testes de rede CAN dedicados ao **Peugeot 508 II (2020)**.

![App Logo](app_logo.jpg)

---

## 🏎️ Funcionalidades Principais

### 1. Manómetro de Pressão de Turbo em Tempo Real (Universal)
* Leitura do sensor MAP (Manifold Absolute Pressure - PID `010B`) e subtração automática da pressão atmosférica de referência (PID `0133`).
* Manómetro circular desportivo customizado com:
  * Zona de vácuo (-1.00 a 0.00 Bar)
  * Zona de sobrepressão normal (0.00 a 1.50 Bar)
  * Zona de pico / *overboost* em vermelho neon (1.50 a 3.00 Bar)
  * Marcador de pico máximo registado (*Peak Hold*) com botão para repor.
* Cartões digitais de telemetria: RPM (`010C`), Velocidade real em km/h (`010D`), Temperatura do líquido de refrigeração em °C (`0105`) e Pressão absoluta no coletor em kPa.

### 2. Cronómetro de Aceleração 0 aos 100 km/h
* Sistema de arranque automático: basta armar o teste com o carro parado. Assim que a velocidade passa de 0 para >0 km/h, o cronómetro arranca.
* Medição milissegundo a milissegundo.
* Bloqueio automático aos 100 km/h e gravação para a lista de histórico de arranques.
* Registo do Melhor Tempo (*Personal Best*).

### 3. Diagnóstico e Scanner de Avarias (DTC)
* Leitura de avarias ativas da centralina do motor (Modo `03`).
* Tradução automática dos códigos OBD-II para português (ex: `P0234` - Sobrepressão de turbo, `P2463` - Saturação de FAP/filtro de partículas, etc.).
* Botão para apagar códigos de avaria da memória da centralina e desligar a luz do motor (*Check Engine* - Modo `04`).

### 4. Laboratório Peugeot 508 & Rede CAN (Secção de Cores)
* **Simulação de Modo Sport:** Envio de tramas CAN para simular a mudança de modo de condução e colocar o quadrante digital e o ecrã em vermelho desportivo.
* **Leitura UDS do Ecrã NAC:** Interrogação dos identificadores de exibição e temas (`ATSH 7B3`, `22 21 00`).
* **Consola de Linha de Comandos ELM327:** Permite enviar comandos personalizados diretamente à carrinha com registo de log de resposta em tempo real.

### 5. Modo Demonstração (Demo Mode)
* Inclui um interruptor "Demo" na barra superior para simular acelerações dinâmicas, subida de pressão de turbo e testes de 0-100 mesmo que esteja em casa, sem o carro por perto.

---

## 🛠️ Como Abrir e Executar no Android Studio

1. Abra o **Android Studio**.
2. Clique em **File > Open...** e selecione a pasta:
   `c:\Users\cac_7\OneDrive\Área de Trabalho\508`
3. O Android Studio irá reconhecer o projeto e descarregar as dependências automaticamente através do Gradle.
4. Ligue o seu telemóvel Android por cabo USB ao computador (com a *Depuração USB / USB Debugging* ativa) ou use o emulador.
5. Clique no botão verde **Run (▶)** no topo do Android Studio.

---

## 📡 Como Ligar ao Adaptador OBD2

1. Ligue o adaptador OBD2 na ficha de diagnóstico da carrinha (por baixo do volante à esquerda no Peugeot 508).
2. Ligue a ignição da carrinha.
3. No telemóvel Android, vá a **Definições > Bluetooth** e emparelhe com o dispositivo (geralmente chamado `OBDII`, `ELM327` ou `Vgate`). O código PIN é habitualmente `1234` ou `0000`.
4. Abra a app **Teste de Pressão**, clique no botão **LIGAR OBD** no canto superior direito e selecione o adaptador da lista.
