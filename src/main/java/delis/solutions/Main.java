package delis.solutions;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.FileWriter;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Класс для запуска программы вывода всех индексов слова "Пьер" в романе Л.Н.Толстого "Война и мир".
 */

public class Main
{
    static Map<Integer, String> wordCountMap = new HashMap<>();
    static Queue<Integer> queue = new PriorityBlockingQueue<>();
    static CountDownLatch latch;

    public static void main( String[] args ) {

        try {
            // Прописываем путь к файлу, создаем экемпляр класса PDDocument для работы с пдф
            File novel = new File("src/main/resources/WarAndPeace.pdf");
            PDDocument pdfDocument = PDDocument.load(novel);

            //Извлекаем текст из пдф файла
            PDFTextStripper pdfTextStripper = new PDFTextStripper();
            String docText = pdfTextStripper.getText(pdfDocument);

            // Вызываем метод для поиска слов, разделенных дефисов при переносе на другую строку, и соединения в единое слово
            String combinedText = processCombineWords(docText);

            //Вызываем метод для поиска всех слов и добавления в hashmap
            wordCountMap = filterWords(combinedText);

            // Создаем итератор для перебора hasmap всех слов в романе
            Iterator<Map.Entry<Integer,String>> iterator = wordCountMap.entrySet().iterator();
            int quarterSize = wordCountMap.size() / 4;
            int count = 0;

            ConcurrentHashMap<Integer,String> threadMap1 = new ConcurrentHashMap<>();
            ConcurrentHashMap<Integer,String> threadMap2 = new ConcurrentHashMap<>();
            ConcurrentHashMap<Integer,String> threadMap3 = new ConcurrentHashMap<>();
            ConcurrentHashMap<Integer,String> threadMap4 = new ConcurrentHashMap<>();

            //Разделяем исходный hasmap на 4 равные части для потоков
            while (iterator.hasNext()) {
                Map.Entry<Integer, String> entry = iterator.next();
                if (count < quarterSize) {
                    threadMap1.put(entry.getKey(), entry.getValue());
                } else if (count < quarterSize * 2) {
                    threadMap2.put(entry.getKey(), entry.getValue());
                } else if (count < quarterSize * 3) {
                    threadMap3.put(entry.getKey(), entry.getValue());
                } else {
                    threadMap4.put(entry.getKey(), entry.getValue());
                }
                count++;
            }

            // Создаем CountDownLatch с числом потоков
            int countThread = 4;
            latch = new CountDownLatch(countThread);

            //Запускаем счетчик слова "Пьер"  в 4 потоках
            for(int i = 0; i < countThread; i++)
            {
                switch (i)
                {
                    case 0: {
                        WordIndex wordIndex1 = new WordIndex(threadMap1, queue);
                        wordIndex1.start();
                        break;
                    }
                    case 1: {
                        WordIndex wordIndex2 = new WordIndex(threadMap2, queue);
                        wordIndex2.start();
                        break;
                    }
                    case 2: {
                        WordIndex wordIndex3 = new WordIndex(threadMap3, queue);
                        wordIndex3.start();
                        break;
                    }
                    case 3: {
                        WordIndex wordIndex4 = new WordIndex(threadMap4, queue);
                        wordIndex4.start();
                        break;
                    }
                }
            }

            // Ожидаем завершения работы всех потоков
            latch.await();

            // Выводим индексы слова "Пьер"
            FileWriter writer = new FileWriter("src/main/resources/output.txt");
            while (!queue.isEmpty()) {
                System.out.println(queue.poll());
                writer.write(queue.poll() + "\n");
            }
            System.out.println("Данные записаны в файл output.txt");

            pdfDocument.close();
        }

        catch (Exception e)
        {
            System.err.println("Произошла ошибка при выполнении программы:");
            e.printStackTrace();
        }
    }

    private static Map<Integer,String> filterWords(String text) {

        /**
         * Этот метод выполняет поиск всех слов (без учета спец.симоволов и цифр) и добавления в hashmap
         * @param text String текста, полученного из метода processCombineWords
         * @return Hashmap всех слов и их индексов
         * @see processCombineWords
         */

        // Используем регулярное выражение для поиска слов
        Pattern pattern = Pattern.compile("[а-яА-Яa-zA-Z]+");
        Matcher matcher = pattern.matcher(text);

        // Собираем найденные слова в hashmap
        int index = 0;
        while (matcher.find()) {
            wordCountMap.put(index,matcher.group());
            index++;
        }
        return wordCountMap;
    }

    private static String processCombineWords(String text) {

        /**
         * Этот метод выполняет поиск слов, разделенных дефисов при переносе на другую строку, и соединения в единое слово
         * @param text String текста из пдф файла всего романа
         * @return String готового текста
         */

        // Используем регулярное выражение для поиска слов, разделенных дефисом
        Pattern pattern = Pattern.compile("(\\p{L}+)-\\s*\\R(\\p{L}+)");
        Matcher matcher = pattern.matcher(text);

        // Заменяем слова, разделенные дефисом, на одно цельное слово
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, matcher.group(1) + matcher.group(2));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

}
