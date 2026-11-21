/*
 * ThermaBio — modelo CAD paramétrico del tanque y serpentín helicoidal
 * Abrir con OpenSCAD (gratuito; el archivo no utiliza librerías externas).
 *
 * Las dimensiones principales se expresan en mm. El volumen de caldo va en L.
 * El diámetro de hélice es el diámetro de la línea central del tubo.
 */

$fn = 48;

// Parámetros de diseño (sincronizados con el caso de ejemplo de la APK)
tank_internal_d = 1100;
tank_working_h = 1400;
tank_wall = 4;
working_volume_l = 1200;

coil_center_d = 550;
tube_od = 25.4;
tube_wall = 1.65;
pitch = 50;
turns = 17.661;
radial_clearance_min = 160;
axial_clearance = tube_od;

show_cutaway_vessel = true;
show_liquid = true;
show_connections = true;

// Dimensiones derivadas
tube_id = tube_od - 2 * tube_wall;
coil_height = turns * pitch;
liquid_volume_mm3 = working_volume_l * 1000000;
tank_cross_section = PI * tank_internal_d * tank_internal_d / 4;
liquid_height = liquid_volume_mm3 / tank_cross_section;
tank_capacity_l = tank_cross_section * tank_working_h / 1000000;
radial_clearance_actual = (tank_internal_d - coil_center_d - tube_od) / 2;
coil_base_z = tank_wall + axial_clearance + tube_od / 2;
coil_top_z = coil_base_z + coil_height;

echo(str("ID tubo = ", tube_id, " mm"));
echo(str("Altura de líquido cilíndrica = ", liquid_height, " mm"));
echo(str("Altura de hélice centro a centro = ", coil_height, " mm"));
echo(str("Holgura radial real = ", radial_clearance_actual, " mm"));
echo(str("Capacidad cilíndrica = ", tank_capacity_l, " L"));

assert(tube_id > 0, "El espesor deja un diámetro interior no positivo.");
assert(pitch >= tube_od, "El paso debe ser al menos el diámetro exterior del tubo.");
assert(coil_center_d > tube_od, "El diámetro de hélice debe superar el diámetro del tubo.");
assert(radial_clearance_actual >= radial_clearance_min, "El serpentín no conserva la holgura radial mínima.");
assert(working_volume_l <= tank_capacity_l, "El volumen de caldo excede la capacidad cilíndrica del tanque.");
assert(coil_top_z + tube_od / 2 <= tank_wall + liquid_height, "La hélice no queda completamente sumergida.");

module tank_shell() {
    difference() {
        cylinder(d=tank_internal_d + 2 * tank_wall, h=tank_working_h + 2 * tank_wall);
        translate([0, 0, tank_wall])
            cylinder(d=tank_internal_d, h=tank_working_h + tank_wall + 0.1);
    }
}

module tank_view() {
    if (show_cutaway_vessel) {
        intersection() {
            tank_shell();
            // Retira la mitad frontal para mostrar el serpentín dentro del recipiente.
            translate([-tank_internal_d, 0, -1])
                cube([2 * tank_internal_d, tank_internal_d + 2 * tank_wall, tank_working_h + 2 * tank_wall + 2]);
        }
    } else {
        color([0.25, 0.30, 0.32, 0.22]) tank_shell();
    }
}

module helical_sweep(diameter) {
    translate([0, 0, coil_base_z])
        linear_extrude(
            height=coil_height,
            twist=360 * turns,
            slices=max(ceil(turns * 32), 64),
            convexity=10
        )
            translate([coil_center_d / 2, 0])
                circle(d=diameter, $fn=32);
}

module rod_between(a, b, radius) {
    hull() {
        translate(a) sphere(r=radius, $fn=16);
        translate(b) sphere(r=radius, $fn=16);
    }
}

module refrigerant_coil() {
    // El barrido interior es sustracción geométrica para que el tubo no sea macizo.
    difference() {
        helical_sweep(tube_od);
        helical_sweep(tube_id);
    }

    if (show_connections) {
        start = [coil_center_d / 2, 0, coil_base_z];
        end_angle = 360 * turns;
        end_x = coil_center_d / 2 * cos(end_angle);
        end_y = coil_center_d / 2 * sin(end_angle);
        end_z = coil_base_z + coil_height;
        end = [end_x, end_y, end_z];
        shell_radius = tank_internal_d / 2 - tank_wall;
        outlet = [shell_radius * cos(end_angle), shell_radius * sin(end_angle), end_z];
        inlet = [shell_radius, 0, coil_base_z];
        color([0.03, 0.48, 0.61]) rod_between(inlet, start, tube_od / 2);
        color([0.80, 0.09, 0.56]) rod_between(end, outlet, tube_od / 2);
    }
}

module liquid_fill() {
    if (show_liquid) {
        color([0.16, 0.70, 0.76, 0.20])
            translate([0, 0, tank_wall])
                cylinder(d=tank_internal_d - 1, h=liquid_height);
    }
}

module model() {
    tank_view();
    liquid_fill();
    color([0.00, 0.68, 0.72]) refrigerant_coil();
}

model();
