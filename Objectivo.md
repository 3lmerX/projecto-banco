# projecto-banco
Projecto Escolar de um Sistema de Transacções Bancárias Distribuído. 

Os serviços financeiros digitais, como a banca móvel e as carteiras móveis, permitem abrircontas e movimentar dinheiro sem ir a uma agência. Por trás de cada operação existe umsistema distribuído: vários serviços que comunicam entre si, guardam dados e têm demanter os saldos correctos, mesmo quando muitos clientes operam ao mesmo tempo.Neste trabalho simula-se um pequeno banco digital, sem agências nem ATMs, onde odinheiro entra por depósito e circula por transferência.
Objectivo:
desenvolver um sistema simples de transacções bancárias com três partes:
1.Backoffice (web):
o funcionário regista clientes e abre as suas contas.
2.Backend (microserviços):
guarda os dados e aplica as regras do negócio.
3.Aplicação móvel:
o cliente abre a sua conta com o número da conta e o PIN e transferedinheiro de uma conta para outra; o valor sai da conta de origem e aparece na conta dedestino.
