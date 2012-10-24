for f in *average.txt
do
        name=`expr "$f" : '\(.*\)-average\.txt'`
	gnuplot -e "set terminal png truecolor font 'ptm, 12';
                    set output 'graphs/$name.png';
                    set xlabel 'Generations';
                    set ylabel 'Fitness';
                    set tics in;
                    set grid;
                    plot '$name-average.txt' title 'average' smooth unique with lines, 
                         '$name-best.txt' title 'best' smooth unique with lines;"
done
