package br.com.bjorn.dto;

public class DocumentResponse {
    private Long id;
    private String originalFilename;
    private String sourceType;

    public DocumentResponse(Long id, String originalFilename, String sourceType) {
        this.id = id;
        this.originalFilename = originalFilename;
        this.sourceType = sourceType;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }
}
