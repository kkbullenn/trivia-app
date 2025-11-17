export function getContextPath() {
    const pathParts = window.location.pathname.split('/');
    pathParts.pop();
    return pathParts.join('/') || '';
}

export async function sendMultipartFormData(url, formData) {
    const response = await fetch(url, {
        method: 'POST',
        body: formData, // The FormData object is directly used as the body
    });

    if (!response.ok) {
        console.error(response);
        throw Error()
    }

    return await response.json();
}