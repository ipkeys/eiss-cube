import { useState, useEffect, useRef, useCallback, useMemo } from 'react';
import { Button, useRecordContext, useDataProvider, useNotify } from 'react-admin';
import L from 'leaflet';
import { MapContainer, TileLayer, Marker, Popup, useMap } from 'react-leaflet';
import { red, grey } from '@mui/material/colors';
import { Box, alpha } from '@mui/material';
import Icon from '@mui/icons-material/EditLocation';

import 'leaflet.awesome-markers/dist/leaflet.awesome-markers.css';
import 'leaflet.awesome-markers/dist/leaflet.awesome-markers.js';

const blueMarker = L.AwesomeMarkers.icon({
	icon: 'circle',
	prefix: 'fa',
	markerColor: 'blue'
});

const init_location = {
	lat: 40.32444602981903,
	lng: -74.07683856203221
};

const init_zoom = 10;

const CubeMarkerMap = (props: any) => {
	const record = useRecordContext(props);
	const notify = useNotify();
	const dataProvider = useDataProvider();
	const [location, setLocation] = useState(init_location);
	const [draggable, setDraggable] = useState(false);
	const markerRef = useRef(null);
	const map = useMap();

	useEffect(() => {
		let position = record && record.location;
		if (position === undefined) {
			position = init_location;
		}
		setLocation(position);
		map.setView(position, init_zoom);
	}, [record, map]);

	const toggleDraggable = useCallback(() => {
		setDraggable((d) => !d)
	}, []);

	const updateLocation = useMemo(() => ({
		dragend() {
			const mrk = markerRef.current;
			if (mrk != null) {
				// @ts-ignore
				const new_position = mrk.getLatLng();
				setLocation(new_position);
			}
		},
	}), []);

	if (!record) return null;

	const onSaveLocation = () => {
		// @ts-ignore
		const new_position = markerRef.current.getLatLng();

		dataProvider.update('cubes/location', {
			id: record.id,
			data: new_position,
			previousData: record
		})
		.then(() => {
			notify(`${record.name} - Location updated`, {type: 'info'});
			setLocation(new_position);
			map.flyTo(new_position);
		})
		.catch((e) => {
			notify(`Cannot update location - ${e}`, {type: 'warning'});
		});
	};

	const note = draggable && <i style={{color: grey[900]}}>Hold and drag the Marker to new location...</i>;

	const btn = draggable
	? <Button label='Fix the new location' fullWidth onClick={onSaveLocation}
		sx={{
			color: red[500],
			'&:hover': {
				backgroundColor: alpha(red[50], 0.5),
				'@media (hover: none)': {
					backgroundColor: 'transparent',
				},
			}
		}}
	>
		<Icon />
	</Button>
	: <Button label='Change location' fullWidth
		sx={{
			color: red[500],
			'&:hover': {
				backgroundColor: alpha(red[50], 0.5),
				'@media (hover: none)': {
					backgroundColor: 'transparent',
				},
			}
		}}
	>
		<Icon />
	</Button>;

	return (
		<Box>
			<TileLayer url='https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png'
				attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
			/>
			<Marker
				draggable={draggable}
				eventHandlers={updateLocation}
				position={location}
				icon={blueMarker}
				ref={markerRef}
			>
				<Popup minWidth={270} closeButton={false}>
					<span onClick={toggleDraggable}>
					<i>ICCID:</i> <b>{record.deviceID}</b>
					<br/>
					<i>Name:</i> {record.name}
					<br/>
					<i>Customer:</i> {record.customerID}
					<br/>
					<a href={`#/commands?filter={"cubeID":"${record.id}"}&page=1&perPage=10&sort=created&order=DESC`}>Commands</a>
					<br/>
					<a href={`#/reports?displayedFilters={"cubeID":true}&filter={"cubeID":"${record.id}"}&order=ASC&page=1&perPage=10&sort=cubeID`}>Report</a>
					<br/>
					<hr/>
					{ note }
					<br/>
					{ btn }
					</span>
				</Popup>
			</Marker>
		</Box>
	);
}

const CubeMap = () => (
	<MapContainer center={init_location} zoom={init_zoom}>
		<CubeMarkerMap />
	</MapContainer>
);

export default CubeMap;
