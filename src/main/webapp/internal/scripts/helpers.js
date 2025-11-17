export function getContextPath() {
    const pathParts = window.location.pathname.split('/');
    pathParts.pop();
    return pathParts.join('/') || '';
}

export async function sendMultipartFormData(url, formData) {
    try {
        const response = await fetch(url, {
            method: 'POST',
            body: formData, // The FormData object is directly used as the body
        });

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        return await response.json();
    } catch (error) {
        console.error('Error:', error);
        throw Error(error.message)
    }
}