import { useState, useEffect, useRef, useCallback, useMemo } from 'react';
import { makeStyles, Theme } from '@material-ui/core/styles';
import { Button, useRecordContext, useDataProvider, useNotify } from 'react-admin';
import L from 'leaflet';
import { useMap, MapContainer, LayersControl, TileLayer, Marker, Popup } from 'react-leaflet';
import { red, grey } from '@material-ui/core/colors';
import { alpha } from '@material-ui/core/styles/colorManipulator';
import Icon from '@material-ui/icons/EditLocation';

import 'leaflet.awesome-markers/dist/leaflet.awesome-markers.css';
import 'leaflet.awesome-markers/dist/leaflet.awesome-markers.js';

const { BaseLayer } = LayersControl;

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

const useStyles = makeStyles((theme: Theme) => ({ 
    button: {
        color: red[500],
        '&:hover': {
            backgroundColor: alpha(red[50], 0.5),
            '@media (hover: none)': {
                backgroundColor: 'transparent',
            },
        }
    }
}));

const CubeMarkerMap = (props: any) => {
    const classes = useStyles();
    const notify = useNotify();
    const dataProvider = useDataProvider();
    const record = useRecordContext(props);
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
            const marker = markerRef.current;
            if (marker != null) {              
                // @ts-ignore
                const new_position = marker.getLatLng();
                setLocation(new_position);
            }
        },
    }), []);

    const onSaveLocation = () => {
        // @ts-ignore
        const new_position = markerRef.current.getLatLng();

        dataProvider.update('cubes/location', {
            id: record.id, 
            data: new_position,
            previousData: record 
        })                    
        .then(() => {
            notify(`${record.name} - Location updated`, 'info');
            setLocation(new_position);
            map.flyTo(new_position);
        })
        .catch((e) => {
            notify(`Cannot update location - ${e}`, 'warning');
        });
    };

    const note = draggable && <i style={{color: grey[900]}}>Hold and drag the Marker to new location...</i>;

    const btn = draggable
    ? <Button className={classes.button} label='Fix the new location' fullWidth onClick={onSaveLocation}>
        <Icon />
    </Button>
    : <Button className={classes.button} label='Change location' fullWidth >
        <Icon />
    </Button>;
    
    return (
        <>
            <LayersControl position='topright'>
                <BaseLayer checked name='Color'>
                    <TileLayer
                        attribution='&copy; <a href="http://osm.org/copyright">OpenStreetMap</a> contributors'
                        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                    />
                </BaseLayer>
                <BaseLayer name='Black And White'>
                    <TileLayer
                        attribution='&copy; <a href="http://osm.org/copyright">OpenStreetMap</a> contributors'
                        url='http://{s}.tiles.wmflabs.org/bw-mapnik/{z}/{x}/{y}.png'
                    />
                </BaseLayer>
                <Marker
                    draggable={draggable}
                    eventHandlers={updateLocation}
                    position={location}
                    icon={blueMarker}
                    ref={markerRef}
                >
                    <Popup minWidth={270}>
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
            </LayersControl>
        </>
    );
}

const CubeMap = (props: any) => {
    return (
        <MapContainer
            animate={true}
            center={init_location}
            zoom={init_zoom}
            scrollWheelZoom={false}
        >
            <CubeMarkerMap {...props} />
        </MapContainer>
    );
}

export default CubeMap;
