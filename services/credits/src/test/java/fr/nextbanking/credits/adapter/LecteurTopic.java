package fr.nextbanking.credits.adapter;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;

/**
 * Lit un topic depuis son début et rend le premier message portant la clé donnée.
 */
public final class LecteurTopic {

    private LecteurTopic() {
    }

    public static Optional<ConsumerRecord<String, String>> chercher(String serveurs, String topic, String cle,
            Duration delai) {
        Map<String, Object> config = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, serveurs,
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        try (KafkaConsumer<String, String> lecteur = new KafkaConsumer<>(config)) {
            List<TopicPartition> partitions = IntStream.range(0, lecteur.partitionsFor(topic).size())
                    .mapToObj(p -> new TopicPartition(topic, p)).toList();
            lecteur.assign(partitions);
            lecteur.seekToBeginning(partitions);
            Instant fin = Instant.now().plus(delai);
            while (Instant.now().isBefore(fin)) {
                for (ConsumerRecord<String, String> message : lecteur.poll(Duration.ofMillis(200))) {
                    if (cle.equals(message.key())) {
                        return Optional.of(message);
                    }
                }
            }
            return Optional.empty();
        }
    }
}
