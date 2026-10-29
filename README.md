# 🚀 Teste de Pressão — App Android de Telemetria e Diagnóstico OBD2

Aplicação Android desenvolvida em **Kotlin** e **Jetpack Compose** para monitorização em tempo real de turbocompressor, teste de aceleração dos 0 aos 100 km/h, leitura/limpeza de códigos de erro, dashboard avançado de múltiplos sensores e ferramentas dedicadas ao **Peugeot 508 II (2020)**.

![App Logo](app_logo.jpg)

---

## 🏎️ Funcionalidades Principais (Versão 1.1.0)

### 1. Manómetro de Pressão de Turbo em Tempo Real (Universal)
* Leitura em tempo real do sensor MAP (`010B`) e cálculo de sobrepressão relativa em Bar (subtraindo a pressão atmosférica `0133`).
* Manómetro circular animado de alta precisão (-1.00 a +3.00 Bar).
* Memória de pressão máxima (*Peak Hold*) com botão de reposição.
* Cartões digitais instantâneos de RPM (`010C`), Velocidade (`010D`), Temperatura do líquido de refrigeração (`0105`) e Pressão do coletor.

### 2. Novo Dashboard de Sensores com Filtros
Aba dedicada com grelha de cartões em tempo real para todos os sensores suportados na carrinha:
* **Temperatura do Óleo do Motor (`015C`):** Essencial para saber quando o motor do 508 atingiu a temperatura ideal para puxar.
* **Tensão da Bateria e Alternador (`0142`):** Monitorização em tempo real da saúde da bateria de 12V e carga do alternador (~14.2V).
* **Carga do Motor (`0104`):** Percentagem de esforço/binário solicitado à centralina.
* **Temperatura do Ar de Admissão / IAT (`010F`):** Medição da eficiência do intercooler.
* **Posição do Acelerador (`0111`):** Percentagem do pedal.
* **Fluxo de Massa de Ar / MAF (`0110`):** Leitura em gramas por segundo (g/s).
* **Nível de Combustível (`012F`):** Percentagem no depósito.
* **Tempo de Motor Ligado (`011F`):** Cronómetro da viagem em minutos e segundos.
* **Filtros Rápidos:** *Todos*, *Motor*, *Temperaturas*, *Elétrica* e *Dinâmica*.

### 3. Ecrã de Definições & Otimização de Velocidade
* **Manter Ecrã Sempre Ligado:** Interruptor nas definições que impede o ecrã do telemóvel de bloquear ou apagar enquanto conduz.
* **Ativar/Desativar Sensores:** Permite desligar sensores que não pretenda monitorizar. Quanto menos sensores estiverem ativos ao mesmo tempo, maior é a taxa de atualização (Hz) do manómetro do turbo e do velocímetro!

### 4. Atualizador Integrado Diretamente na App (In-App Updater)
* Nunca mais precisa de descarregar ficheiros manualmente pelo navegador!
* A app verifica diretamente as atualizações no seu GitHub com 1 toque.
* Quando houver uma nova versão, descarrega e abre o instalador automaticamente sobre a versão existente, mantendo todas as suas preferências.

### 5. Cronómetro de Aceleração 0 aos 100 km/h
* Deteção automática de arranque com o carro parado.
* Medição precisa com milissegundos e bloqueio automático aos 100 km/h.
* Registo de melhor tempo e histórico de puxadas.

### 6. Diagnóstico e Leitura de Avarias (DTC)
* Leitura de erros da centralina (Modo `03`) com descrição em português.
* Apagar códigos de avaria e desligar a luz do motor (Modo `04`).

### 7. Laboratório Peugeot 508 & Modo Demonstração
* Consola interativa de comandos CAN / UDS com registo de log de resposta da carrinha.
* Interruptor **Demo** para testar toda a interface, manómetros e cronómetro 0-100 mesmo fora do carro.

---

## 📦 Como Instalar e Atualizar

1. Descarregue o ficheiro **`TesteDePressao.apk`** diretamente do GitHub no seu telemóvel Android.
2. Toque em instalar.
3. Para as próximas versões, basta ir ao separador **Definições** dentro da própria app e tocar em **"Verificar Atualizações"**!
