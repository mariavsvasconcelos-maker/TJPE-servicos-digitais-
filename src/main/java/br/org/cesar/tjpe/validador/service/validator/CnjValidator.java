package br.org.cesar.tjpe.validador.service.validator;

import org.springframework.stereotype.Component;
import java.math.BigInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validador oficial de numeração única de processos judiciais
 * de acordo com a Resolução nº 65/2008 do Conselho Nacional de Justiça (CNJ).
 * 
 * Formato: NNNNNNN-DD.AAAA.J.TR.OOOO
 * Para o Tribunal de Justiça de Pernambuco: J=8 (Justiça Estadual) e TR=17 (TJPE).
 */
@Component
public class CnjValidator {

    private static final Pattern CNJ_PATTERN = 
            Pattern.compile("^(\\d{7})-?(\\d{2})\\.?(\\d{4})\\.?(\\d{1})\\.?(\\d{2})\\.?(\\d{4})$");

    public boolean isValid(String cnj) {
        if (cnj == null) return false;
        String limpo = cnj.trim();
        Matcher matcher = CNJ_PATTERN.matcher(limpo);
        if (!matcher.matches()) {
            return false;
        }

        String sequencial = matcher.group(1);
        String dvInformado = matcher.group(2);
        String ano = matcher.group(3);
        String justica = matcher.group(4);
        String tribunal = matcher.group(5);
        String origem = matcher.group(6);

        // Regra do CNJ: calcular DV esperado via Módulo 97
        String dvCalculado = calcularDigitoVerificador(sequencial, ano, justica, tribunal, origem);
        return dvInformado.equals(dvCalculado);
    }

    public boolean isTjpe(String cnj) {
        if (cnj == null) return false;
        String limpo = cnj.replaceAll("\\D", "");
        if (limpo.length() != 20) return false;

        // J=8 (Estadual), TR=17 (Pernambuco)
        char justica = limpo.charAt(13);
        String tribunal = limpo.substring(14, 16);
        return justica == '8' && "17".equals(tribunal);
    }

    public String calcularDigitoVerificador(String sequencial, String ano, String justica, String tribunal, String origem) {
        // Fórmula CNJ: D = 97 - ((NNNNNNNAAAAJTROOOO * 100) % 97)
        String numeroParaCalculo = sequencial + ano + justica + tribunal + origem + "00";
        BigInteger num = new BigInteger(numeroParaCalculo);
        int resto = num.mod(BigInteger.valueOf(97)).intValue();
        int dv = 97 - resto;
        return String.format("%02d", dv);
    }

    public String formatar(String cnj) {
        if (cnj == null) return null;
        String limpo = cnj.replaceAll("\\D", "");
        if (limpo.length() == 20) {
            return String.format("%s-%s.%s.%s.%s.%s",
                    limpo.substring(0, 7),
                    limpo.substring(7, 9),
                    limpo.substring(9, 13),
                    limpo.substring(13, 14),
                    limpo.substring(14, 16),
                    limpo.substring(16, 20));
        }
        return cnj;
    }
}
