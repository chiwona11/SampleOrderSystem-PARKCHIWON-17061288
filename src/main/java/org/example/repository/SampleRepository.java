package org.example.repository;

import org.example.model.Sample;
import org.example.util.JsonFileUtil;

import java.io.File;
import java.util.List;
import java.util.Optional;

public class SampleRepository implements CrudRepository<Sample, String> {

    private final File file;

    public SampleRepository(File file) {
        this.file = file;
    }

    @Override
    public void save(Sample entity) {
        List<Sample> list = JsonFileUtil.readList(file, Sample.class);
        list.add(entity);
        JsonFileUtil.writeList(file, list);
    }

    @Override
    public Optional<Sample> findById(String id) {
        return JsonFileUtil.readList(file, Sample.class).stream()
                .filter(s -> s.getId().equals(id))
                .findFirst();
    }

    @Override
    public List<Sample> findAll() {
        return JsonFileUtil.readList(file, Sample.class);
    }

    @Override
    public void update(Sample entity) {
        List<Sample> list = JsonFileUtil.readList(file, Sample.class);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId().equals(entity.getId())) {
                list.set(i, entity);
                break;
            }
        }
        JsonFileUtil.writeList(file, list);
    }

    @Override
    public void deleteById(String id) {
        List<Sample> list = JsonFileUtil.readList(file, Sample.class);
        list.removeIf(s -> s.getId().equals(id));
        JsonFileUtil.writeList(file, list);
    }

    public boolean existsById(String id) {
        return findById(id).isPresent();
    }
}
