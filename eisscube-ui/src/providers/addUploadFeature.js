/**
 * Convert a `File` object returned by the upload input into a base 64 string.
 */
const convertFileToBase64 = file => new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.readAsDataURL(file.rawFile);

    reader.onload = () => resolve(reader.result);
    reader.onerror = reject;
});

/**
 * For posts update only, convert uploaded file in base 64 and attach it to
 * the `store` sent property, with `src` and `title` attributes.
 */
const addUploadFeature = requestHandler => (type, resource, params) => {
    if (type === 'CREATE' && (resource === 'software')) {
        // type = components
        if (params.data.components && params.data.components.rawFile instanceof File) {
            const newFile = params.data.components;
            const title = params.data.components.title;

            return Promise.all([newFile].map(convertFileToBase64))
            .then(base64File => base64File.map(file64 => ({
                src: file64,
                title
            })))
            .then(components => requestHandler(type, resource, {
                ...params,
                data: {
                    components
                }
            }));
        }
    }
    return requestHandler(type, resource, params);
};

export default addUploadFeature;
