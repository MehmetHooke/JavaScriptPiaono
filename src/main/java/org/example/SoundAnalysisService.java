package org.example;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.WaveformSimilarityBasedOverlapAdd;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchProcessor;
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm;


import java.util.*;

public class SoundAnalysisService {

    private static final Map<String, Double> noteFrequencies = new HashMap<>();
    private static final double FREQUENCY_TOLERANCE = 2.0; // 1 Hz'lik tolerans
    private String lastDetectedNote = ""; // Son algılanan nota

    // Müzikte beklenen minimum ve maksimum frekans değerleri
    private static final double MIN_MUSIC_FREQUENCY = 65.41;  // Do2'nin frekansı
    private static final double MAX_MUSIC_FREQUENCY = 4186.01; // Do8'in frekansı



    static {

        noteFrequencies.put("Do1", 32.70);
        noteFrequencies.put("Do#1", 34.65);
        noteFrequencies.put("Re1", 36.71);
        noteFrequencies.put("Re#1", 38.89);
        noteFrequencies.put("Mi1", 41.20);
        noteFrequencies.put("Fa1", 43.65);
        noteFrequencies.put("Fa#1", 46.25);
        noteFrequencies.put("Sol1", 49.00);
        noteFrequencies.put("Sol#1", 51.91);
        noteFrequencies.put("La1", 55.00);
        noteFrequencies.put("La#1", 58.27);
        noteFrequencies.put("Si1", 61.74);


        noteFrequencies.put("Do2", 65.41);
        noteFrequencies.put("Do#2", 69.30);
        noteFrequencies.put("Re2", 73.42);
        noteFrequencies.put("Re#2", 77.78);
        noteFrequencies.put("Mi2", 82.41);
        noteFrequencies.put("Fa2", 87.31);
        noteFrequencies.put("Fa#2", 92.50);
        noteFrequencies.put("Sol2", 98.00);
        noteFrequencies.put("Sol#2", 103.83);
        noteFrequencies.put("La2", 110.00);
        noteFrequencies.put("La#2", 116.54);
        noteFrequencies.put("Si2", 123.47);


        noteFrequencies.put("Do3", 130.81);
        noteFrequencies.put("Do#3", 138.59);
        noteFrequencies.put("Re3", 146.83);
        noteFrequencies.put("Re#3", 155.56);
        noteFrequencies.put("Mi3", 164.81);
        noteFrequencies.put("Fa3", 174.61);
        noteFrequencies.put("Fa#3", 185.00);
        noteFrequencies.put("Sol3", 196.00);
        noteFrequencies.put("Sol#3", 207.65);
        noteFrequencies.put("La3", 220.00);
        noteFrequencies.put("La#3", 233.08);
        noteFrequencies.put("Si3", 246.94);



        // 4. Oktav
        noteFrequencies.put("Do4", 261.63);
        noteFrequencies.put("Do#4", 277.18);
        noteFrequencies.put("Re4", 293.66);
        noteFrequencies.put("Re#4", 311.13);
        noteFrequencies.put("Mi4", 329.63);
        noteFrequencies.put("Fa4", 349.23);
        noteFrequencies.put("Fa#4", 369.99);
        noteFrequencies.put("Sol4", 392.00);
        noteFrequencies.put("Sol#4", 415.30);
        noteFrequencies.put("La4", 440.00);
        noteFrequencies.put("La#4", 466.16);
        noteFrequencies.put("Si4", 493.88);

        // 5. Oktav
        noteFrequencies.put("Do5", 523.25);
        noteFrequencies.put("Do#5", 554.37);
        noteFrequencies.put("Re5", 587.33);
        noteFrequencies.put("Re#5", 622.25);
        noteFrequencies.put("Mi5", 659.25);
        noteFrequencies.put("Fa5", 698.46);
        noteFrequencies.put("Fa#5", 739.99);
        noteFrequencies.put("Sol5", 783.99);
        noteFrequencies.put("Sol#5", 830.61);
        noteFrequencies.put("La5", 880.00);
        noteFrequencies.put("La#5", 932.33);
        noteFrequencies.put("Si5", 987.77);



        noteFrequencies.put("Do6", 1046.50);
        noteFrequencies.put("Do#6", 1108.73);
        noteFrequencies.put("Re6", 1174.66);
        noteFrequencies.put("Re#6", 1244.51);
        noteFrequencies.put("Mi6", 1318.51);
        noteFrequencies.put("Fa6", 1396.91);
        noteFrequencies.put("Fa#6", 1479.98);
        noteFrequencies.put("Sol6", 1567.98);
        noteFrequencies.put("Sol#6", 1661.22);
        noteFrequencies.put("La6", 1760.00);
        noteFrequencies.put("La#6", 1864.66);
        noteFrequencies.put("Si6", 1975.53);



        noteFrequencies.put("Do7", 2093.00);
        noteFrequencies.put("Do#7", 2217.46);
        noteFrequencies.put("Re7", 2349.32);
        noteFrequencies.put("Re#7", 2489.02);
        noteFrequencies.put("Mi7", 2637.02);
        noteFrequencies.put("Fa7", 2793.83);
        noteFrequencies.put("Fa#7", 2959.96);
        noteFrequencies.put("Sol7", 3135.96);
        noteFrequencies.put("Sol#7", 3322.44);
        noteFrequencies.put("La7", 3520.00);
        noteFrequencies.put("La#7", 3729.31);
        noteFrequencies.put("Si7", 3951.07);

    }

    public void analyzeAudio(String filePath) {
        try {
            double defaultParam1 = 512;  // İşleme parametreleri
            double defaultParam2 = 128;

            WaveformSimilarityBasedOverlapAdd wsola = new WaveformSimilarityBasedOverlapAdd(
                    WaveformSimilarityBasedOverlapAdd.Parameters.musicDefaults(defaultParam1, defaultParam2)
            );

            // AudioDispatcher oluşturuluyor
            AudioDispatcher dispatcher = AudioDispatcherFactory.fromPipe(filePath, 44100, 1024, 0);

            // PitchDetectionHandler tanımlanıyor
            PitchDetectionHandler pdh = (pitchDetectionResult, e) -> {
                final float pitchInHz = pitchDetectionResult.getPitch();

                // Frekansın belirli bir müzik aralığında olup olmadığını kontrol ediyoruz
                if (pitchInHz >= MIN_MUSIC_FREQUENCY && pitchInHz <= MAX_MUSIC_FREQUENCY) {
                    // Harmonik analiz ekleyerek notayı doğrulama
                    if (isHarmonicMatch(pitchInHz)) {
                        logNoteWithTime(pitchInHz, e.getTimeStamp());
                    }
                }
            };

            dispatcher.addAudioProcessor(new PitchProcessor(PitchEstimationAlgorithm.YIN, 44100, 1024, pdh));
            dispatcher.run();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void logNoteWithTime(double frequency, double timeStamp) {
        String currentNote = findClosestNote(frequency);

        // Eğer notada belirgin bir değişiklik varsa veya ilk defa algılanıyorsa
        if (!currentNote.equals(lastDetectedNote)) {
            System.out.println("Zaman: " + String.format("%.2f", timeStamp) + " sn - Algılanan Pitch: " +
                    String.format("%.2f", frequency) + " Hz - Nota: " + currentNote);
            lastDetectedNote = currentNote; // Son algılanan notayı güncelle
        }
    }

    public String findClosestNote(double frequency) {
        String closestNote = "";
        double smallestDifference = Double.MAX_VALUE;

        for (Map.Entry<String, Double> entry : noteFrequencies.entrySet()) {
            double difference = Math.abs(entry.getValue() - frequency);

            // Eğer frekans farkı toleransın altındaysa en yakın notayı seç
            if (difference < smallestDifference && difference <= FREQUENCY_TOLERANCE) {
                smallestDifference = difference;
                closestNote = entry.getKey();
            }
        }

        return closestNote;
    }

    /**
     * Harmonik uyumluluğu kontrol eden metod
     */
    private boolean isHarmonicMatch(double frequency) {
        String closestNote = findClosestNote(frequency);

        // Eğer bir nota bulunmadıysa veya geçerli bir nota değilse, harmonik kontrol yapılmaz
        if (closestNote.isEmpty()) {
            return false;
        }

        double baseFrequency = noteFrequencies.get(closestNote);

        // İlk üç harmonik frekansını hesaplayıp tolerans sınırında kontrol et
        for (int i = 2; i <= 4; i++) {
            double harmonicFrequency = baseFrequency * i;
            double difference = Math.abs(harmonicFrequency - frequency * i);

            // Harmoniklerin tolerans aralığında olup olmadığını kontrol et
            if (difference > FREQUENCY_TOLERANCE) {
                return false;
            }
        }
        return true;
    }



    public static void main(String[] args) {
        SoundAnalysisService service = new SoundAnalysisService();
        service.analyzeAudio("C:/Users\\hoke6\\OneDrive\\Masaüstü\\doremi1.wav"); // Dosya yolunu güncelleyin
    }
}

//C:\Users\hoke6\OneDrive\Masaüstü\Do Re Me.wav
//C:/Users/hoke6/OneDrive/Masaüstü/bitirmeprojesi/ses.wav
//C:\Users\hoke6\OneDrive\Masaüstü\BellaCiao.wav