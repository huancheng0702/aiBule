export function getEmergencyList() {
	return uni.$get('/appApi/emergency/list')
}

export function getEmergencyRelations() {
	return uni.$get('/appApi/emergency/relations')
}

export function addEmergencyContact(data) {
	return uni.$post('/appApi/emergency/add', data)
}

export function editEmergencyContact(data) {
	return uni.$put('/appApi/emergency/edit', data)
}

export function removeEmergencyContact(contactId) {
	return uni.$delete('/appApi/emergency/' + contactId, { contactId })
}
