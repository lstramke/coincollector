export type ExportRequest = {
    fileType: string;
};

const API_BASE = "/api/v1/export";

export const exportService = {
    getExportUrl(request: ExportRequest): string {
        const params = new URLSearchParams({
            fileType: request.fileType
        });

        return `${API_BASE}?${params.toString()}`;
    }
};