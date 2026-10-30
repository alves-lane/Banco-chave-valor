package org.example;

// Importações necessárias do Firebase
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

// Importações para criar uma credencial "falsa" para o emulador local
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;

// Importações básicas do Java
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class FirebaseKeyValueApp {

    // Nome fictício do projeto, usado apenas pelo emulador local
    private static final String NOME_DO_PROJETO = "meu-projeto";

    // Endereço do banco de dados local emulador rodando na porta 9000
    private static final String ENDERECO_DO_BANCO = "http://localhost:9000/?ns=" + NOME_DO_PROJETO;

    public static void main(String[] args) throws IOException, InterruptedException {

        // Essa linha avisa o firebase que vamo usar o e mulador local e que precisa vir ANTES de inicializar o Firebase
        System.setProperty("FIREBASE_DATABASE_EMULATOR_HOST", "localhost:9000");

       //cria uma credencial falsa
        // O emulador local não valida credenciais reais, então é preciso criar uma "de mentira"
        Date dataDeExpiracao = Date.from(
                LocalDateTime.now().plusYears(1).atZone(ZoneId.systemDefault()).toInstant()
        );

        AccessToken tokenFalso = new AccessToken("mock-token", dataDeExpiracao);
        GoogleCredentials credenciaisFalsas = GoogleCredentials.create(tokenFalso);

        //incia o firebase
        FirebaseOptions opcoes = FirebaseOptions.builder()
                .setCredentials(credenciaisFalsas)
                .setDatabaseUrl(ENDERECO_DO_BANCO)
                .setProjectId(NOME_DO_PROJETO)
                .build();

        FirebaseApp.initializeApp(opcoes);

        // pega a referência da tabela
        FirebaseDatabase banco = FirebaseDatabase.getInstance();
        DatabaseReference referencia = banco.getReference("minhaTabela");

        // primeira opeereção
        System.out.println("--- Iniciando operações (chave1 e chave2) ---");

        cadastrar(referencia, "chave1", "Primeiro valor");
        cadastrar(referencia, "chave2", "Segundo valor");

        consultar(referencia, "chave1");

        excluir(referencia, "chave2");

        // Espera 2 segundos para as operações assíncronas terminarem
        Thread.sleep(2000);

        System.out.println("--- Verificando após exclusão (chave2) ---");
        consultar(referencia, "chave2");

        // segunda operação
        System.out.println("--- Iniciando operações (produtos) ---");

        cadastrar(referencia, "produto_01", "Notebook ");
        cadastrar(referencia, "produto_02", "Mouse ");

        consultar(referencia, "produto_01");

        Thread.sleep(2000);

        excluir(referencia, "produto_02");
        System.out.println("Solicitada a exclusão do produto_02...");

        Thread.sleep(2000);

        System.out.println("--- Verificando após exclusão (produto_02) ---");
        consultar(referencia, "produto_02");

        // mantém o programa rodando, se não o programa fecharia antes dos callbacks assíncronos rodarem
        new CountDownLatch(1).await();
    }

    //métodos equivalentes a get, put e delete
    private static void cadastrar(DatabaseReference referencia, String chave, String valor) {
        // Monta um "pacote" com o valor e a hora atual
        Map<String, Object> dados = new HashMap<>();
        dados.put("valor", valor);
        dados.put("timestamp", System.currentTimeMillis());

        // setValue salva os dados no caminho "minhaTabela/<chave>"
        referencia.child(chave).setValue(dados, (erro, snapshot) -> {
            if (erro == null) {
                System.out.println("Cadastro bem-sucedido: " + chave + " = " + valor);
            } else {
                System.err.println("Falha no cadastro: " + erro.getMessage());
            }
        });
    }


    private static void consultar(DatabaseReference referencia, String chave) {
        referencia.child(chave).addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Pega só o campo "valor" do objeto salvo
                    Object valor = snapshot.child("valor").getValue();
                    System.out.println("Consulta [" + chave + "]: " + valor);
                } else {
                    System.out.println("Consulta [" + chave + "]: Item não encontrado");
                }
            }

            @Override
            public void onCancelled(DatabaseError erro) {
                System.err.println("Falha na consulta: " + erro.getMessage());
            }
        });
    }


    private static void excluir(DatabaseReference referencia, String chave) {
        referencia.child(chave).removeValue((erro, snapshot) -> {
            if (erro == null) {
                System.out.println("Exclusão bem-sucedida: " + chave);
            } else {
                System.err.println("Falha na exclusão: " + erro.getMessage());
            }
        });
    }
}