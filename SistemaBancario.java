import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class Banco {
    private final Lock lock = new ReentrantLock();

    public void transferir(Conta origem, Conta destino, double valor) {
        lock.lock();
        try {
            if (origem.getSaldo() >= valor) {
                origem.debitar(valor);
                destino.creditar(valor);
                System.out.println("Transferência de R$" + valor + " de " + origem.getCliente() +
                        " para " + destino.getCliente());
            } else {
                System.out.println("Saldo insuficiente para transferência de " + origem.getCliente());
            }
        } finally {
            lock.unlock();
        }
    }
}

class Conta {
    private final String cliente;
    private double saldo;
    private final Lock lock = new ReentrantLock();

    public Conta(String cliente, double saldo) {
        this.cliente = cliente;
        this.saldo = saldo;
    }

    public String getCliente() {
        return cliente;
    }

    public double getSaldo() {
        return saldo;
    }

    public void creditar(double valor) {
        lock.lock();
        try {
            saldo += valor;
        } finally {
            lock.unlock();
        }
    }

    public void debitar(double valor) {
        lock.lock();
        try {
            saldo -= valor;
        } finally {
            lock.unlock();
        }
    }
}

class Loja {
    private final Conta conta;
    private final double salarioFuncionario = 1400 * 2;

    public Loja(Conta conta) {
        this.conta = conta;
    }

    public void pagarFuncionarios() {
        synchronized (conta) {
            if (conta.getSaldo() >= salarioFuncionario) {
                conta.debitar(salarioFuncionario);
                System.out.println("Salário pago aos funcionários da loja.");
            } else {
                System.out.println("Saldo insuficiente para pagar os funcionários da loja.");
            }
        }
    }

    public Conta getConta() {
        return conta;
    }
}

class Funcionario extends Thread {
    private final Conta contaSalario;
    private final Conta contaInvestimento;

    public Funcionario(Conta contaSalario, Conta contaInvestimento) {
        this.contaSalario = contaSalario;
        this.contaInvestimento = contaInvestimento;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            synchronized (contaSalario) {
                try {
                    contaSalario.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            double salario = 1400;
            double investimento = salario * 0.2;
            synchronized (contaInvestimento) {
                contaInvestimento.creditar(investimento);
            }
            System.out.println("Funcionário recebeu salário e investiu R$" + investimento);
        }
    }
}

class Cliente extends Thread {
    private final Conta conta;
    private final Loja[] lojas;

    public Cliente(Conta conta, Loja[] lojas) {
        this.conta = conta;
        this.lojas = lojas;
    }

    @Override
    public void run() {
        while (conta.getSaldo() > 0) {
            double valor = Math.random() < 0.5 ? 100 : 200;
            Loja loja = lojas[(int) (Math.random() * lojas.length)];
            synchronized (loja) {
                Banco banco = new Banco();
                banco.transferir(conta, loja.getConta(), valor);
            }
            try {
                Thread.sleep((long) (Math.random() * 1000)); // Simula tempo de compra
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

public class SistemaBancario {
    public static void main(String[] args) {
        Conta contaBanco = new Conta("Banco", 0);
        Conta contaLoja1 = new Conta("Loja1", 0);
        Conta contaLoja2 = new Conta("Loja2", 0);
        Loja loja1 = new Loja(contaLoja1);
        Loja loja2 = new Loja(contaLoja2);
        Funcionario[] funcionarios = new Funcionario[4];
        for (int i = 0; i < 4; i++) {
            Conta contaSalario = new Conta("Funcionario" + (i + 1), 0);
            Conta contaInvestimento = new Conta("Investimento" + (i + 1), 0);
            funcionarios[i] = new Funcionario(contaSalario, contaInvestimento);
            funcionarios[i].start();
        }
        Cliente[] clientes = new Cliente[5];
        for (int i = 0; i < 5; i++) {
            Conta contaCliente = new Conta("Cliente" + (i + 1), 1000);
            clientes[i] = new Cliente(contaCliente, new Loja[]{loja1, loja2});
            clientes[i].start();
        }
        // Simula o funcionamento das lojas e o pagamento dos funcionários
        while (true) {
            try {
                Thread.sleep(5000); // Simula intervalo de 5 segundos para pagamento dos funcionários
                loja1.pagarFuncionarios();
                loja2.pagarFuncionarios();
                synchronized (funcionarios) {
                    for (Funcionario funcionario : funcionarios) {
                        synchronized (funcionario) {
                            funcionario.notify();
                        }
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
