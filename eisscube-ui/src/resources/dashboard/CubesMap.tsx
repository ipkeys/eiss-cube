import L from 'leaflet';
import { MapContainer, LayersControl, TileLayer, Marker, Popup } from 'react-leaflet';
//import MarkerClusterGroup from 'react-leaflet-markercluster';
import { red, green } from '@material-ui/core/colors';
import Paper from '@material-ui/core/Paper';

import 'leaflet.awesome-markers/dist/leaflet.awesome-markers.css';
import 'leaflet.awesome-markers/dist/leaflet.awesome-markers';
import 'react-leaflet-markercluster/dist/styles.min.css';

const { BaseLayer } = LayersControl;

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

const CubesMarkerMap = (props: any) => {
    const {data, ids} = props;

    const markers = ids && ids.map((id: number) => {
        const cube = data[id];
        const location = cube.location ? cube.location : init_location;
        const status = cube.online 
            ? <div style={{color: green[500], textAlign: 'center'}}><b>ONLINE</b></div> 
            : <div style={{color: red[500], textAlign: 'center'}}><b>OFFLINE</b></div>;
        const icon = cube.online ? greenMarker : redMarker;
        return (
            <Marker key={id}
                position={location}
                icon={icon}
            >
                <Popup minWidth={200}>
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
        <>
            <LayersControl position='topright'>
                <BaseLayer checked name="Color">
                    <TileLayer
                        attribution='&copy; <a href="http://osm.org/copyright">OpenStreetMap</a> contributors'
                        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                    />
                </BaseLayer>
                <BaseLayer name="Black And White">
                    <TileLayer
                        attribution='&copy; <a href="http://osm.org/copyright">OpenStreetMap</a> contributors'
                        url="http://{s}.tiles.wmflabs.org/bw-mapnik/{z}/{x}/{y}.png"
                    />
                </BaseLayer>
                {/*
                <MarkerClusterGroup {...props}>
                    { markers }
                </MarkerClusterGroup>
                */}
                    { markers }
            </LayersControl>
        </>
    );
}

const CubesMap = (props: any) => {

    return (
        <Paper>
            <MapContainer
                animate={true}
                center={init_location}
                zoom={init_zoom}
                scrollWheelZoom={false}
            >
                <CubesMarkerMap {...props} />
            </MapContainer>
        </Paper>
    );
}

export default CubesMap;
