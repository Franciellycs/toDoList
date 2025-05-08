package br.com.franciellycorrea.todolist.filter;

import java.io.IOException;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import at.favre.lib.crypto.bcrypt.BCrypt;
import br.com.franciellycorrea.todolist.user.IUserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class FilterTaskAuth extends OncePerRequestFilter {

    @Autowired
    private IUserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        var servletPath = request.getServletPath();

        if (servletPath.equals("/tasks/")) {

            // Pegando dados de autenticação
            var authorization = request.getHeader("Authorization");


            if (authorization == null || !authorization.startsWith("Basic ")) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Header Authorization ausente ou inválido");
                return;
            }

            try {
                // Decodificando
                var authEncoded = authorization.substring("Basic".length()).trim();
                byte[] authDecoded = Base64.getDecoder().decode(authEncoded);

                // Convertendo para String
                var authString = new String(authDecoded);

                // Separando login e senha
                String[] credentials = authString.split(":");

                // Verificando se usuário e senha vieram corretamente
                if (credentials.length != 2) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Formato de credenciais inválido");
                    return;
                }

                String username = credentials[0];
                String password = credentials[1];

                // validando user
                var user = this.userRepository.findByUsername(username);

                if (user == null) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Usuário não encontrado");
                } else {
                    
                    // validando a senha
                    var passwordVirify = BCrypt.verifyer().verify(password.toCharArray(), user.getPassword());
                    if (passwordVirify.verified) { 
                        filterChain.doFilter(request, response);
                    } else {
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Senha inválida");
                    }

                }

            } catch (Exception e) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Erro ao processar autenticação");
            }

        } else {
            // Continua filtragem para outras rotas
            filterChain.doFilter(request, response);
        }
    }
}
