package delis.solutions;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Класс для поиска индексов слова "Пьер".
 */
public class WordIndex extends Thread
{
    private static final String TARGET_WORD = "Пьер";
    private final Map<Integer,String> threadMap;
    private final Queue<Integer> threadQueue;


    public WordIndex (ConcurrentHashMap<Integer,String> threadMap, Queue<Integer> threadQueue)
    {
        this.threadMap = threadMap;
        this.threadQueue = threadQueue;
    }

    //Переписываем метод run, в котором происходит поиск слов "Пьер" и заносится в очередь их индексы
    @Override
    public void run() {
        try
        {
            for (Map.Entry<Integer, String> entry : threadMap.entrySet()) {
                if (Objects.equals(entry.getValue(), TARGET_WORD)) {
                    threadQueue.add(entry.getKey());
                }
            }
        }
        catch (Exception ex)
        {
            System.err.println("Произошла ошибка при выполнении программы:");
            ex.printStackTrace();
        }
        finally {
            // Уменьшаем счетчик на единицу при завершении работы потока
            Main.latch.countDown();
        }
    }
}
