package br.com.eduardobaptista.javaweb2.service;

import br.com.eduardobaptista.javaweb2.model.FotoImovelModel;
import br.com.eduardobaptista.javaweb2.model.ImovelModel;
import br.com.eduardobaptista.javaweb2.repository.FotoImovelRepository;
import br.com.eduardobaptista.javaweb2.repository.ImovelRepository;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
public class FotoImovelService {
    @Autowired
    private FotoImovelRepository fotoImovelRepository;

    @Autowired
    private ImovelRepository imovelRepository;

    @Value("${app.upload.dir}")
    private String appUploadDir;

    public FotoImovelModel salvar (MultipartFile arquivo, Integer imovelId) {
        ImovelModel imovelModel = imovelRepository.findById(imovelId).orElseThrow(() -> new RuntimeException("Imóvel não encontrado"));

        try {
            Path path = Paths.get(appUploadDir, imovelId.toString());
            Files.createDirectories(path);
            String originalName = StringUtils.cleanPath(arquivo.getOriginalFilename());
            String newName = UUID.randomUUID() + "_" + originalName;
            Path destino = path.resolve(newName);
            arquivo.transferTo(destino);

            FotoImovelModel fotoImovelModel = new FotoImovelModel();
            fotoImovelModel.setCaminho(destino.toString());
            fotoImovelModel.setNomeArquivo(newName);
            fotoImovelModel.setImovel(imovelModel);

            return fotoImovelRepository.save(fotoImovelModel);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;

    }

    public void excluir(Integer fotoImovelId) {
        FotoImovelModel fotoImovelModel = fotoImovelRepository.findById(fotoImovelId).orElseThrow(() -> new RuntimeException("Foto não encontrada"));
        Path fileToDelete = Paths.get(fotoImovelModel.getCaminho());
        try {
            Files.delete(fileToDelete);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        fotoImovelRepository.deleteById(fotoImovelId);
    }

    public FotoImovelModel editar (MultipartFile arquivo, Integer fotoImovelId) {
        FotoImovelModel fotoImovelModel = fotoImovelRepository.findById(fotoImovelId).orElseThrow(() -> new RuntimeException("Foto não encontrada"));
        String original = fotoImovelModel.getCaminho();

        try {
            Path path = Paths.get(appUploadDir, fotoImovelModel.getImovel().getId().toString());
            Files.createDirectories(path);
            String originalName = StringUtils.cleanPath(arquivo.getOriginalFilename());
            String newName = UUID.randomUUID() + "_" + originalName;
            Path destino = path.resolve(newName);
            arquivo.transferTo(destino);

            fotoImovelModel.setCaminho(destino.toString());
            fotoImovelModel.setNomeArquivo(newName);

            Path fileToDelete = Paths.get(original);
            Files.delete(fileToDelete);

            return fotoImovelRepository.save(fotoImovelModel);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;

    }

    public void tornarDestaque (Integer fotoImovelId) {
        FotoImovelModel fotoImovelModel = fotoImovelRepository.findById(fotoImovelId).orElseThrow(() -> new RuntimeException("Foto não encontrada"));

        List<FotoImovelModel> fotos = fotoImovelRepository.findByImovelId(fotoImovelModel.getImovel().getId());

        for (FotoImovelModel foto : fotos) {
            foto.setCapa(foto.getId().equals(fotoImovelId));
        }

        fotoImovelRepository.saveAll(fotos);
    }


}
