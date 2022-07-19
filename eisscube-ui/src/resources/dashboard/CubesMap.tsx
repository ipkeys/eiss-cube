import L from 'leaflet';
import { MapContainer, TileLayer, Marker, Popup } from 'react-leaflet';
import MarkerClusterGroup from "./MarkerClusterGroup";
import { red, green } from '@mui/material/colors';
import Paper from '@mui/material/Paper';

import 'leaflet.awesome-markers/dist/leaflet.awesome-markers.css';
import 'leaflet.awesome-markers/dist/leaflet.awesome-markers';

const redMarker = L.AwesomeMarkers.icon({
	icon: 'circle',
	prefix: 'fa',
	markerColor: 'red'
});

const greenMarker = L.AwesomeMarkers.icon({
	icon: 'circle',
	prefix: 'fa',
	markerColor: 'green'
});

const init_location = {
	lat: 40.32444602981903,
	lng: -74.07683856203221
};

const init_zoom = 10;

const CubesMap = (props: any) => {
	const { data } = props;

	const markers = data && data.map((cube: any, index: number) => {
		const location = cube.location ? cube.location : init_location;
		const status = cube.online
			? <div style={{color: green[500], textAlign: 'center'}}><b>ONLINE</b></div>
			: <div style={{color: red[500], textAlign: 'center'}}><b>OFFLINE</b></div>;
		const icon = cube.online ? greenMarker : redMarker;
		return (
			<Marker key={index}
				position={location}
				icon={icon}
			>
				<Popup minWidth={200} closeButton={false}>
					{status}
					<span>
						<i>ICCID:</i> <b>{cube.deviceID}</b>
						<br/>
						<i>Name:</i> <a href={`#/cubes/${cube.id}/show`}>{cube.name}</a>
						<br/>
						<i>Customer:</i> {cube.customerID}
						<br/>
						<a href={`#/commands?filter={"cubeID":"${cube.id}"}&page=1&perPage=10&sort=created&order=DESC`}>Commands</a>
						<br/>
						<a href={`#/reports?displayedFilters={"cubeID":true}&filter={"cubeID":"${cube.id}"}&order=ASC&page=1&perPage=10&sort=cubeID`}>Report</a>
					</span>
				</Popup>
			</Marker>
		);
	});

	return (
		<Paper elevation={10}>
			<MapContainer center={init_location} zoom={init_zoom}>
				<TileLayer url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
					attribution='&copy; <a href="http://osm.org/copyright">OpenStreetMap</a>'
				/>
				<MarkerClusterGroup>
					{markers}
				</MarkerClusterGroup>
			</MapContainer>
		</Paper>
	);
}

export default CubesMap;
